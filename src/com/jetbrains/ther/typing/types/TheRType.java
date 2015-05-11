package com.jetbrains.ther.typing.types;

import com.intellij.openapi.util.text.StringUtil;
import com.jetbrains.ther.psi.api.TheRExpression;
import com.jetbrains.ther.typing.TheRTypeEnvironment;

import java.util.ArrayList;
import java.util.List;

public abstract class TheRType implements Cloneable {
  private List<String> myS3Classes = new ArrayList<String>();

  public static TheRType getMaxType(List<TheRType> types, TheRTypeEnvironment env) {
    TheRType maxType = null;
    for (TheRType type : types) {
      TheRType resolvedType = type.resolveType(env);
      if (maxType == null || TheRType.getOrder(resolvedType) > TheRType.getOrder(maxType)) {
        maxType = resolvedType;
      }
    }
    return maxType;
  }

  public static int getOrder(TheRType type) {
    if (type instanceof TheRNullType) {
      return -1;
    } else if (type instanceof TheRRawType) {
      return 0;
    } else if (type instanceof TheRLogicalType){
      return 1;
    } else if (type instanceof TheRIntegerType) {
      return 2;
    } else if (type instanceof TheRNumericType) {
      return 3;
    } else if (type instanceof TheRComplexType) {
      return 4;
    } else if (type instanceof TheRCharacterType) {
      return 5;
    } else if (type instanceof TheRUnknownType) {
      return 6;
    } else {
      throw new IllegalArgumentException("Incorrect type: " + type.getName());
    }
  }

  @Override
  public TheRType clone() {
    try {
      TheRType result = (TheRType) super.clone();
      result.myS3Classes = new ArrayList<String>(myS3Classes);
      return result;
    }
    catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  public final String getName() {
    if (myS3Classes.isEmpty()) {
      return getCanonicalName();
    }
    return getCanonicalName() + "[" + StringUtil.join(myS3Classes, ", ") + "]";
  }

  public abstract String getCanonicalName();

  @SuppressWarnings("SimplifiableIfStatement")
  @Override
  public boolean equals(Object o) {
    if (o == null || !(o instanceof TheRType)) {
      return false;
    }
    return ((TheRType)o).getName().equals(getName());
  }

  public TheRType resolveType(TheRTypeEnvironment env) {
    return clone();
  }

  public TheRType getSubscriptionType(List<TheRExpression> expressions, boolean isSingleBracket) {
    return TheRUnknownType.INSTANCE;
  }

  @Override
  public int hashCode() {
    return getName().hashCode();
  }

  @Override
  public String toString() {
    return getName();
  }

  public TheRType afterSubscriptionType(List<TheRExpression> arguments, TheRType valueType) {
    // TODO : valueType is union
    if (arguments.isEmpty()) {
      return this;
    }
    return TheRUnknownType.INSTANCE;
  }

  public TheRType getElementTypes() {
    return TheRUnknownType.INSTANCE;
  }

  public List<String> getS3Classes() {
    return myS3Classes;
  }

  public TheRType replaceS3Types(List<String> s3Classes) {
    TheRType result = clone();
    result.myS3Classes = new ArrayList<String>(s3Classes);
    return result;
  }
}
