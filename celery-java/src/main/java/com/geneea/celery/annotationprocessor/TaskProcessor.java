package com.geneea.celery.annotationprocessor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.kohsuke.MetaInfServices;
import com.geneea.celery.CeleryTask;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementScanner8;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

@MetaInfServices(Processor.class)
@SupportedAnnotationTypes("com.geneea.celery.CeleryTask")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class TaskProcessor extends AbstractProcessor {

    private final VelocityEngine ve;

    public TaskProcessor() {
        Properties props = new Properties();
        URL url = TaskProcessor.class.getClassLoader().getResource("velocity.properties");
        try {
            props.load(url.openStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ve = new VelocityEngine(props);
        ve.init();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {


        for (Element elem : roundEnv.getElementsAnnotatedWith(CeleryTask.class)) {
            assert elem.getKind() == ElementKind.CLASS;

            TypeElement taskClassElem = (TypeElement) elem;

            if (taskClassElem.getNestingKind().isNested()) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "Error processing CeleryTask " + taskClassElem + ". Only top-level classes are supported as tasks.");
                break;
            }

            final List<Map<Object, Object>> methods = findMethods(taskClassElem);

            PackageElement packageElement = (PackageElement) taskClassElem.getEnclosingElement();
            Name packageName = packageElement.getQualifiedName();

            writeProxy(taskClassElem, methods, packageName);
            writeLoader(taskClassElem, packageName);
        }
        return true;
    }

    private List<Map<Object, Object>> findMethods(Element elem) {
        final List<Map<Object, Object>> methods = new ArrayList<>();

        elem.accept(new ElementScanner8<Void, Void>() {
            @Override
            public Void visitExecutable(ExecutableElement e, Void aVoid) {
                if (!ImmutableList.of("<init>", "<clinit>", "").contains(e.getSimpleName().toString())) {

                    List<Map<Object, Object>> parameters = e.getParameters().stream().map((param) ->
                            ImmutableMap.<Object, Object>of(
                                    "simpleName", param.getSimpleName(),
                                    "type", convert(param.asType())
                        )).collect(Collectors.toList());

                    methods.add(ImmutableMap.of("simpleName", e.getSimpleName(),
                            "returnType", convert(e.getReturnType()),
                            "parameters", parameters));
                }
                return super.visitExecutable(e, aVoid);
            }
        }, null);
        return methods;
    }

    private void writeLoader(TypeElement elem, Name packageName) {
        Name binaryName = processingEnv.getElementUtils().getBinaryName(elem);
        try {
            JavaFileObject file = processingEnv.getFiler().createSourceFile(binaryName + "Loader", elem);

            VelocityContext vc = new VelocityContext();

            vc.put("taskName", elem.getSimpleName());
            vc.put("packageName", packageName);

            Template vt = ve.getTemplate("com/geneea/celery/templates/TaskLoader.vm");

            Writer writer = file.openWriter();
            vt.merge(vc, writer);
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeProxy(TypeElement elem, List<Map<Object, Object>> methods, Name packageName) {
        Name binaryName = processingEnv.getElementUtils().getBinaryName(elem);
        try {
            JavaFileObject file = processingEnv.getFiler().createSourceFile(binaryName + "Proxy", elem);

            VelocityContext vc = new VelocityContext();

            vc.put("taskName", elem.getSimpleName());
            vc.put("packageName", packageName);
            vc.put("methods", methods);

            Template vt = ve.getTemplate("com/geneea/celery/templates/TaskProxy.vm");

            Writer writer = file.openWriter();
            vt.merge(vc, writer);
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String convert(TypeMirror type) {
        if (type.getKind().isPrimitive()) {
            return processingEnv.getTypeUtils().boxedClass((PrimitiveType) type).toString();
        } else if (type.getKind() == TypeKind.VOID) { // for some reason wasn't catched by the previous condition
            return "java.lang.Void";
        } {
            return type.toString();
        }
    }
}
