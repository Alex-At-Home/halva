/**
 * Copyright 2016 Jordan Zimmerman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.soabase.halva.processor.alias;

import io.soabase.halva.processor.Environment;
import io.soabase.halva.processor.Pass;
import io.soabase.halva.processor.WorkItem;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class PassAnalyze implements Pass
{
    private final Environment environment;
    private final List<WorkItem> workItems;

    PassAnalyze(Environment environment, List<WorkItem> workItems)
    {
        this.environment = environment;
        this.workItems = workItems;
    }

    @Override
    public Optional<Pass> process()
    {
        List<AliasSpec> specs = new ArrayList<>();
        workItems.forEach(item -> {
            do
            {
                Element element = item.getElement();
                if ( element.getKind() != ElementKind.INTERFACE )
                {
                    environment.error(element, "@TypeAlias can only be applied to interfaces");
                    break;
                }

                TypeElement typeElement = (TypeElement)element;
                if ( typeElement.getInterfaces().size() != 1 )
                {
                    environment.error(element, "@TypeAlias interfaces must extend a type that is to be aliased");
                }
                TypeMirror parentType = typeElement.getInterfaces().get(0);

                if ( typeElement.getEnclosedElements().size() != 0 )
                {
                    environment.error(element, "@TypeAlias must be completely empty");
                    break;
                }

                specs.add(new AliasSpec(typeElement, item.getAnnotationReader(), (DeclaredType)parentType));
            } while ( false );
        });
        return Optional.of(new PassCreate(environment, specs));
    }
}
