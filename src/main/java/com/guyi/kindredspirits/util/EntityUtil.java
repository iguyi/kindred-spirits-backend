package com.guyi.kindredspirits.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EntityUtil {

    /**
     * <p>
     * 传入两个对象, 比较它们在逻辑上是否相等.<br>
     * 对于传入的两个对象, 即使它们引用类型可以不同, 仍然有可能被判定为逻辑相等.
     * </p>
     * <p>
     * 在这里, 传入的两个对象在逻辑上相等需要满足的条件: <br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
     * 1. 被 Objects.equals() 判断相等. <br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
     * 2. 或者同时满足下列条件: <br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
     * 1) 两个对象存在共同的属性. <br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
     * 2) 两个对象共同属性的值对应相等.
     * </p>
     * <p>
     *     在对数据库进行更新时, 可以用于判断 entity 和 entityVo 之间是否逻辑相等, 避免一些无意义的数据库更新操作.
     * </p>
     *
     * @param newObject - 新对象
     * @param oldObject - 原对象
     * @return Objects.equals() 判断 newObject 和 oldObject 相等或者它们之间存在共同属性且共同属性的值对应相等时, 返回 true.
     */
    public static boolean entityEq(Object newObject, Object oldObject) {
        if (Objects.equals(newObject, oldObject)) {  // 两个对象实际是否相等
            return true;
        }
        if (newObject == null || oldObject == null) {  // 到这里, 只要有一个 null, 一定不等
            return false;
        }
        Class<?> newClass = newObject.getClass();
        Class<?> oldClass = oldObject.getClass();

        Field[] newDeclaredFields = newClass.getDeclaredFields();
        Field[] oldDeclaredFields = oldClass.getDeclaredFields();
        List<String> newDeclaredFieldNameList = new ArrayList<>();
        List<String> oldDeclaredFieldNameList = new ArrayList<>();
        for (Field newDeclaredField : newDeclaredFields) {
            newDeclaredFieldNameList.add(newDeclaredField.getName());
        }
        for (Field oldDeclaredField : oldDeclaredFields) {
            oldDeclaredFieldNameList.add(oldDeclaredField.getName());
        }
        for (String oldDeclaredFieldName : oldDeclaredFieldNameList) {  // 判断两对象具体属性的值是否存在差异
            if (newDeclaredFieldNameList.contains(oldDeclaredFieldName)) {  // oldObject 的当前属性是否存在与 newObject 中
                try {
                    Field newDeclaredField = newClass.getDeclaredField(oldDeclaredFieldName);
                    newDeclaredField.setAccessible(true);
                    Field oldDeclaredField = oldClass.getDeclaredField(oldDeclaredFieldName);
                    oldDeclaredField.setAccessible(true);
                    try {
                        Object newValue = newDeclaredField.get(newObject);
                        Object oldValue = oldDeclaredField.get(oldObject);
                        if (!Objects.equals(newValue, oldValue)) {  // 两个对象都有这个属性, 并且属性值存在差异
                            return false;
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

    // todo 判定除了指定属性外, 其他属性是否都为 null

}
