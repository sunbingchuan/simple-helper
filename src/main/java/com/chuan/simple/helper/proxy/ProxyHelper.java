/*
 * Copyright 2018-2021 Bingchuan Sun.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.chuan.simple.helper.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @see BaseProxy
 * @see InstanceProxy
 * @see WholeProxy
 * @see WrapProxy
 */
public final class ProxyHelper {

    private static final InstanceProxy instanceProxy = new InstanceProxy();

    private static final WholeProxy wholeProxy = new WholeProxy();

    private static final WrapProxy wrapProxy = new WrapProxy();

    /**
     * @see InstanceProxy#instance(InvocationHandler, Class[])
     */
    public static Object instance(InvocationHandler handler,
            Class<?>[] interfaceClasses) {
        return instanceProxy.instance(handler, interfaceClasses);
    }

    /**
     * @see InstanceProxy#instance(InvocationHandler, Class)
     */
    public static Object instance(InvocationHandler handler, Class<?> parent) {
        return instanceProxy.instance(handler, parent);
    }

    /**
     * @see InstanceProxy#instance(InvocationHandler, Class, Class[])
     */
    public static Object instance(InvocationHandler handler, Class<?> parent,
            Class<?>[] interfaceClasses) {
        return instanceProxy.instance(handler, parent, interfaceClasses);
    }

    /**
     * @see InstanceProxy#instance(InvocationHandler, Class, Method[])
     */
    public static Object instance(InvocationHandler handler, Class<?> parent,
            Method[] overrides) {
        return instanceProxy.instance(handler, parent, overrides);
    }

    /**
     * @see InstanceProxy#instance(InvocationHandler, Class, Method[], Class[])
     */
    public static Object instance(InvocationHandler handler, Class<?> parent,
            Method[] overrides, Class<?>[] interfaceClasses) {
        return instanceProxy.instance(handler, parent, overrides,
                interfaceClasses);
    }

    /**
     * @see WholeProxy#proxy(Class, InvocationHandler)
     */
    public static Object proxy(Class<?> target, InvocationHandler handler) {
        return wholeProxy.proxy(target, handler);
    }

    /**
     * @see WholeProxy#proxyClass(Class, InvocationHandler)
     */
    public static Class<?> proxyClass(Class<?> target,
            InvocationHandler handler) {
        return wholeProxy.proxyClass(target, handler);
    }

    /**
     * @see WrapProxy#wrap(Class, Object)
     */
    public static Object wrap(Class<?> target, Object instance) {
        return wrapProxy.wrap(target, instance);
    }

    /**
     * @see WrapProxy#wrap(Class, Object, InvocationHandler)
     */
    public static Object wrap(Class<?> target, Object instance,
            InvocationHandler handler) {
        return wrapProxy.wrap(target, instance, handler);
    }

    /**
     * @see WrapProxy#wrapClass(Class, Object, InvocationHandler)
     */
    public static Class<?> wrapClass(Class<?> target, Object instance,
            InvocationHandler handler) {
        return wrapProxy.wrapClass(target, instance, handler);
    }

    public static WholeProxy getWholeProxy() {
        return wholeProxy;
    }

    public static InstanceProxy getInstanceProxy() {
        return instanceProxy;
    }

    public static WrapProxy getWrapProxy() {
        return wrapProxy;
    }

    private ProxyHelper() {
    }

    public synchronized static void setClassLoader(ClassLoader classLoader){
        instanceProxy.setClassLoader(classLoader);
        wholeProxy.setClassLoader(classLoader);
        wrapProxy.setClassLoader(classLoader);
    }

    public synchronized static ClassLoader getClassLoader(){
        return instanceProxy.getClassLoader();
    }

    /**
     * Clear the generated classes by
     * changing the class loader and clear
     * the class cache.
     *
     * <pre>
     * Conditions of GC to reclaim class:
     * 1.All instances of the class was reclaimed;
     * 2.The class loader of it was reclaimed;
     * 3.Not any references of it.
     * </pre>
     *
     * @param newClassLoader  the new class loader
     */
    public static void refresh(ClassLoader newClassLoader){
        setClassLoader(newClassLoader);
        instanceProxy.clear();
        wrapProxy.clear();
        wrapProxy.clear();
    }
    
}
