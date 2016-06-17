package io.soabase.halva.processor.caseclass;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import io.soabase.halva.processor.Environment;
import io.soabase.halva.processor.Pass;
import io.soabase.halva.tuple.ClassTuplable;
import io.soabase.halva.tuple.Tuplable;
import javax.lang.model.element.Modifier;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

class Pass2CreateClass implements Pass
{
    private final Environment environment;
    private final List<CaseClassSpec> specs;
    private final Templates templates;

    Pass2CreateClass(Environment environment, List<CaseClassSpec> specs)
    {
        this.environment = environment;
        this.specs = specs;
        templates = new Templates(environment);
    }

    @Override
    public Optional<Pass> process()
    {
        specs.forEach(this::processOneSpec);
        return Optional.empty();
    }

    private void processOneSpec(CaseClassSpec spec)
    {
        String packageName = environment.getPackage(spec.getAnnotatedElement());
        ClassName originalQualifiedClassName = ClassName.get(packageName, spec.getAnnotatedElement().getSimpleName().toString());
        ClassName qualifiedClassName = ClassName.get(packageName, environment.getCaseClassSimpleName(spec.getAnnotatedElement(), spec.getAnnotationReader()));

        environment.log("Generating " + spec.getAnnotationReader().getName() + " for " + originalQualifiedClassName + " as " + qualifiedClassName);

        Collection<Modifier> modifiers = environment.getModifiers(spec.getAnnotatedElement());
        TypeName baseType = TypeName.get(spec.getAnnotatedElement().asType());
        TypeSpec.Builder builder = TypeSpec.classBuilder(qualifiedClassName)
            .addSuperinterface(baseType)
            .addSuperinterface(Serializable.class)
            .addSuperinterface(Tuplable.class)
            .addSuperinterface(ClassTuplable.class)
            .addModifiers(modifiers.toArray(new Modifier[modifiers.size()]));

        Optional<List<TypeVariableName>> typeVariableNames = environment.addTypeVariableNames(builder, spec.getAnnotatedElement());
        boolean isCaseObject = spec.getAnnotationReader().getName().equals("CaseObject");
        boolean json = spec.getAnnotationReader().getBoolean("json");
        if ( json )
        {
            AnnotationSpec annotationSpec = AnnotationSpec.builder(ClassName.get("com.fasterxml.jackson.databind.annotation", "JsonDeserialize"))
                .addMember("builder", "$T.class", templates.getBuilderClassName(qualifiedClassName, typeVariableNames))
                .build();
            builder.addAnnotation(annotationSpec);
        }
        spec.getItems().forEach(item -> templates.addItem(item, builder, json));
        templates.addConstructor(spec, builder, isCaseObject);
        if ( isCaseObject )
        {
            templates.addObjectInstance(builder, qualifiedClassName, typeVariableNames);
        }
        else
        {
            templates.addBuilder(spec, builder, qualifiedClassName, json, typeVariableNames);
            templates.addCopy(builder, qualifiedClassName, typeVariableNames);
            templates.addApplyBuilder(spec, builder, qualifiedClassName, typeVariableNames);
        }
        templates.addEquals(spec, builder, qualifiedClassName);
        templates.addTuple(spec, builder);
        templates.addHashCode(spec, builder);
        templates.addDebugString(spec, builder, qualifiedClassName);
        templates.addToString(spec, builder, qualifiedClassName);
        templates.addClassTuple(spec, builder, qualifiedClassName, json);

        environment.createSourceFile(packageName, originalQualifiedClassName, qualifiedClassName, spec.getAnnotationReader().getName(), builder, spec.getAnnotatedElement());
    }
}
