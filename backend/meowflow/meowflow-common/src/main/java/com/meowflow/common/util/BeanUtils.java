package com.meowflow.common.util;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.util.HashMap;
import java.util.Map;

public final class BeanUtils {

    private BeanUtils() {
    }

    public static String[] getNullPropertyNames(Object source) {
        BeanWrapper bw = new BeanWrapperImpl(source);
        java.beans.PropertyDescriptor[] pds = bw.getPropertyDescriptors();
        java.util.List<String> nullNames = new java.util.ArrayList<>();
        for (java.beans.PropertyDescriptor pd : pds) {
            if (bw.getPropertyValue(pd.getName()) == null) {
                nullNames.add(pd.getName());
            }
        }
        return nullNames.toArray(new String[0]);
    }

    public static void copyPropertiesIgnoreNull(Object src, Object target) {
        org.springframework.beans.BeanUtils.copyProperties(src, target, getNullPropertyNames(src));
    }

    public static Map<String, Object> toMap(Object bean) {
        if (bean == null) {
            return new HashMap<>();
        }
        BeanWrapper bw = new BeanWrapperImpl(bean);
        Map<String, Object> map = new HashMap<>();
        for (java.beans.PropertyDescriptor pd : bw.getPropertyDescriptors()) {
            if ("class".equals(pd.getName())) {
                continue;
            }
            map.put(pd.getName(), bw.getPropertyValue(pd.getName()));
        }
        return map;
    }
}