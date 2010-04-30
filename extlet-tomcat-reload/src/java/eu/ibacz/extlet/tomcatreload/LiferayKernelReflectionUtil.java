/**
 * Copyright (c) 2009-2010 IBA CZ, s. r. o.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package eu.ibacz.extlet.tomcatreload;

import com.liferay.portal.kernel.cache.CacheRegistry;
import com.liferay.portal.kernel.dao.orm.EntityCacheUtil;
import com.liferay.portal.kernel.dao.orm.FinderCacheUtil;
import com.liferay.portal.kernel.deploy.hot.HotDeployUtil;
import com.liferay.portal.kernel.jndi.JNDIUtil;
import com.liferay.portal.kernel.portlet.PortletBagPool;
import com.liferay.portal.kernel.util.InstancePool;
import com.liferay.portal.kernel.util.MethodCache;
import com.liferay.portal.kernel.xml.DocumentException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Map;

/**
 * Class for manipulation with portal-kernel.jar classes using reflection.
 * @author Tomáš Polešovský
 */
public class LiferayKernelReflectionUtil {


    /**
     * Fixes HotDeployUtil class, reinitialize the List of prematureEvents:
     * <pre>
     * private static HotDeployUtil _instance = new HotDeployUtil();
     * private List<HotDeployEvent> _prematureEvents;
     * </pre>
     */
    public void reinitializeHotDeployUtil() throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException, InstantiationException, InvocationTargetException {
        //HotDeployUtil.class:
        //--------------------
        //private static HotDeployUtil _instance = new HotDeployUtil();
        //private List<HotDeployEvent> _prematureEvents;

        /*
         * Get singleton instance of InstancePool
         */
        // get declaration of the field
        Field instanceField = HotDeployUtil.class.getDeclaredField("_instance");
        // set private field accessible to reflection
        instanceField.setAccessible(true);
        // get value of the static field
        HotDeployUtil hotDeployUtil = (HotDeployUtil) instanceField.get(null);

        /*
         * set new list to the hotDeployUtil
         */
        Field propertyField = HotDeployUtil.class.getDeclaredField("_prematureEvents");
        // set private field accessible to reflection
        propertyField.setAccessible(true);
        // set value of the field
        propertyField.set(hotDeployUtil, new ArrayList());


    }

    /**
     * We clear singleton instance for the MethodCache:
     * <pre>
     * private static MethodCache _instance = new MethodCache();
     * </pre>
     */
    public void reinitializeMethodCache() throws NoSuchFieldException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
        //  MethodCache.class:
        //  -----------------
        //  private static MethodCache _instance = new MethodCache();

        /*
         * Get singleton instance field definition
         */
        // get declaration of the field
        Field instanceField = MethodCache.class.getDeclaredField("_instance");
        // set private field accessible to reflection
        instanceField.setAccessible(true);


        /*
         * Create new instance of HotDeployUtil
         */
        Constructor<MethodCache> constructor = (Constructor<MethodCache>) MethodCache.class.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        MethodCache instance = constructor.newInstance();

        /*
         * Assign instance into the static property
         */
        instanceField.set(null, instance);

    }


    /**
     * Clears cache in the class JNDIUtils:
     * <pre>
     * private static Map<String, Object> _cache = new HashMap<String, Object>();
     * </pre>
     */
    public void invalidateJNDIUtil() throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException{
        //    JNDIUtils.class:
        //    ---------------
        //    private static Map<String, Object> _cache = new HashMap<String, Object>();

        /*
         * Get singleton instance of the cache
         */
        // get declaration of the field
        Field cacheField = JNDIUtil.class.getDeclaredField("_cache");
        // set private field accessible to reflection
        cacheField.setAccessible(true);
        // get value of the static field
        Map cacheMap = (Map) cacheField.get(null);

        /*
         * Invalidate
         */
        cacheMap.clear();

    }

    /**
     * Clears cache in the PortletBagPool singleton:
     * <pre>
     * private static PortletBagPool _instance = new PortletBagPool();
     * private Map<String, PortletBag>_portletBagPool;
     * </pre>
     */
    public void invalidatePortletBagPool() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException{
        // PortletBagPool.class:
        // --------------------
        //  private static PortletBagPool _instance = new PortletBagPool();
        //  private Map<String, PortletBag>_portletBagPool;


        /*
         * Get singleton instance of InstancePool
         */
        // get declaration of the field
        Field instanceField = PortletBagPool.class.getDeclaredField("_instance");
        // set private field accessible to reflection
        instanceField.setAccessible(true);
        // get value of the static field
        PortletBagPool instance = (PortletBagPool) instanceField.get(null);

        // get declaration of the field
        Field mapField = PortletBagPool.class.getDeclaredField("_portletBagPool");
        // set private field accessible to reflection
        mapField.setAccessible(true);
        // get value of the static field
        Map map = (Map) mapField.get(instance);

        /*
         * Invalidate
         */
        map.clear();

    }


    /**
     * We access private field ({@see java.util.Map}) of the
     * {@see com.liferay.portal.kernel.cache.CacheRegistry} and call
     * {@see java.util.Map#clear}.<br />
     * <pre>
     * private static Map<String, CacheRegistryItem> _items =
     *     new ConcurrentHashMap<String, CacheRegistryItem>();
     * </pre>
     *
     * Also clear other registry:
     * <pre>
     * CacheRegistry.clear();
     * EntityCacheUtil.clearCache();
     * FinderCacheUtil.clearCache();
     * </pre>
     */
    public void invalidateCacheRegistry() throws DocumentException, IOException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {


        // CacheRegistry.class:
        // --------------------
        //	private static Map<String, CacheRegistryItem> _items =
        //		new ConcurrentHashMap<String, CacheRegistryItem>();


        /*
         * Get singleton instance of InstancePool
         */
        // get declaration of the field
        Field itemsField = CacheRegistry.class.getDeclaredField("_items");
        // set private field accessible to reflection
        itemsField.setAccessible(true);
        // get value of the static field
        Map itemsMap = (Map) itemsField.get(null);

        /*
         * Invalidate
         */
        itemsMap.clear();

        /*
         * Clear all registries
         */
        CacheRegistry.clear();
        EntityCacheUtil.clearCache();
        FinderCacheUtil.clearCache();
    }

    /**
     * First of all we get the private singleton of 
     * {@see com.liferay.portal.kernel.util.InstancePool}. Then we access
     * the cache ({@see java.util.Map}) using private property and call {@see java.util.Map#clear Map.clear}.
     * <pre>
     * private static InstancePool _instance = new InstancePool();
     * private Map<String, Object> _classPool;
     * </pre>
     */
    public void invalidateInstancePool() throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        //  InstancePool.class:
        //  -------------------
        //	private static InstancePool _instance = new InstancePool();
        //
        //	private Map<String, Object> _classPool;

        /*
         * Get singleton instance of InstancePool
         */
        // get declaration of the field
        Field instanceField = InstancePool.class.getDeclaredField("_instance");
        // set private field accessible to reflection
        instanceField.setAccessible(true);
        // get value of the static field
        InstancePool instancePool = (InstancePool) instanceField.get(null);

        /*
         * Get private cache of the InstancePool
         */
        Field classPoolField = InstancePool.class.getDeclaredField("_classPool");
        // set private field accessible to reflection
        classPoolField.setAccessible(true);
        // get value of the static field
        Map classPool = (Map) classPoolField.get(instancePool);

        /*
         * Invalidate the class pool
         */
        classPool.clear();
    }
}
