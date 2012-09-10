/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

/*
 * Created by IntelliJ IDEA.
 * User: max
 * Date: Jan 28, 2002
 * Time: 6:31:08 PM
 * To change template for new class use 
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.intellij.codeInspection.dataFlow.value;

import com.intellij.psi.*;
import com.intellij.util.containers.HashMap;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DfaVariableValue extends DfaValue {
  public static class Factory {
    private final DfaVariableValue mySharedInstance;
    private final HashMap<String,ArrayList<DfaVariableValue>> myStringToObject;
    private final DfaValueFactory myFactory;
    private final MultiMap<DfaVariableValue, DfaVariableValue> myQualifiersToChainedVariables = new MultiMap<DfaVariableValue, DfaVariableValue>();

    Factory(DfaValueFactory factory) {
      myFactory = factory;
      mySharedInstance = new DfaVariableValue(factory);
      myStringToObject = new HashMap<String, ArrayList<DfaVariableValue>>();
    }

    public DfaVariableValue createVariableValue(PsiVariable myVariable, boolean isNegated) {
      return createVariableValue(myVariable, isNegated, null);
    }
    private DfaVariableValue createVariableValue(PsiVariable myVariable, boolean isNegated, @Nullable DfaVariableValue qualifier) {
      mySharedInstance.myVariable = myVariable;
      mySharedInstance.myIsNegated = isNegated;
      mySharedInstance.myQualifier = qualifier;

      String id = mySharedInstance.toString();
      ArrayList<DfaVariableValue> conditions = myStringToObject.get(id);
      if (conditions == null) {
        conditions = new ArrayList<DfaVariableValue>();
        myStringToObject.put(id, conditions);
      }
      else {
        for (DfaVariableValue aVar : conditions) {
          if (aVar.hardEquals(mySharedInstance)) return aVar;
        }
      }

      DfaVariableValue result = new DfaVariableValue(myVariable, isNegated, myFactory, qualifier);
      if (qualifier != null) {
        myQualifiersToChainedVariables.putValue(qualifier, result);
      }
      conditions.add(result);
      return result;
    }

    public List<DfaVariableValue> getAllQualifiedBy(DfaVariableValue value) {
      ArrayList<DfaVariableValue> result = new ArrayList<DfaVariableValue>();
      for (DfaVariableValue directQualified : myQualifiersToChainedVariables.get(value)) {
        result.add(directQualified);
        result.addAll(getAllQualifiedBy(directQualified));
      }
      return result;
    }

    @Nullable
    public DfaVariableValue createFromReference(@NotNull PsiReferenceExpression expression, @NotNull PsiVariable target) {
      PsiExpression qualifier = expression.getQualifierExpression();
      if (qualifier == null) {
        return createVariableValue(target, false, null);
      }

      if (qualifier instanceof PsiReferenceExpression && target instanceof PsiField && target.hasModifierProperty(PsiModifier.FINAL)) {
        PsiElement qTarget = ((PsiReferenceExpression)qualifier).resolve();
        if (qTarget instanceof PsiVariable) {
          DfaVariableValue qualifierValue = createFromReference((PsiReferenceExpression)qualifier, (PsiVariable)qTarget);
          return qualifierValue == null ? null : createVariableValue(target, false, qualifierValue);
        }
      }

      return null;
    }
  }

  private PsiVariable myVariable;
  @Nullable private DfaVariableValue myQualifier;
  private boolean myIsNegated;

  private DfaVariableValue(PsiVariable variable, boolean isNegated, DfaValueFactory factory, @Nullable DfaVariableValue qualifier) {
    super(factory);
    myVariable = variable;
    myIsNegated = isNegated;
    myQualifier = qualifier;
  }

  private DfaVariableValue(DfaValueFactory factory) {
    super(factory);
    myVariable = null;
    myIsNegated = false;
  }

  @Nullable
  public PsiVariable getPsiVariable() {
    return myVariable;
  }

  public boolean isNegated() {
    return myIsNegated;
  }

  public DfaVariableValue createNegated() {
    return myFactory.getVarFactory().createVariableValue(myVariable, !myIsNegated, myQualifier);
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  public String toString() {
    if (myVariable == null) return "$currentException";
    return (myIsNegated ? "!" : "") + myVariable.getName() + (myQualifier == null ? "" : "|" + myQualifier.toString());
  }

  private boolean hardEquals(DfaVariableValue aVar) {
    return aVar.myVariable == myVariable &&
           aVar.myIsNegated == myIsNegated &&
           (myQualifier == null ? aVar.myQualifier == null : myQualifier.hardEquals(aVar.myQualifier));
  }

  @Nullable
  public DfaVariableValue getQualifier() {
    return myQualifier;
  }
}
