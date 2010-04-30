/**
 * Copyright (c) 2000-2010 Liferay, Inc. All rights reserved.
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

package com.liferay.portal.model;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.SAXReader;
import com.liferay.portal.service.ClassNameLocalServiceUtil;
import com.liferay.portal.util.PropsKeys;
import com.liferay.portal.util.PropsUtil;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.net.URL;
import java.io.InputStream;

import org.springframework.core.io.UrlResource;

/**
 * Enables extension of the portal model-hints.xml file using the META-INF/extlet-model-hints.xml.<br />
 * <br />
 * Note: This implementation is the same as the original ModelHintsImpl class,
 * because the ModelHintsImpl have no get/set method for the private fields.<br />
 * <br />
 * There are changed the afterPropertiesSet() method and read() methods: <ul>
 * <li>The afterPropertiesSet() method read all configuration files</li>
 * <li>The read() methods load files using correct classloader</li>
 * </ul>
 *
 * @author Brian Wing Shun Chan
 * @author Tomáš Polešovský
 *
 */
public class ExtletModelHintsImpl implements ModelHints {

	public void afterPropertiesSet() {
		_hintCollections = new HashMap<String, Map<String, String>>();
		_defaultHints = new HashMap<String, Map<String, String>>();
		_modelFields = new HashMap<String, Object>();
		_models = new TreeSet<String>();

		try {
            /*
             * LOAD THE ORIGINAL HINTS
             */
			ClassLoader classLoader = getClass().getClassLoader();

			String[] configs = StringUtil.split(
				PropsUtil.get(PropsKeys.MODEL_HINTS_CONFIGS));

			for (int i = 0; i < configs.length; i++) {
				read(classLoader, configs[i]);
			}

            /*
             * LOAD EXTLET MODEL EXTENSION
             */
            String resourceName = EXTLET_MODEL_RESOURCE_NAME;
            Enumeration<URL> resources = classLoader.getResources(resourceName);
		    while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if(_log.isDebugEnabled()){
                    _log.debug("Loading extlet-model-hints.xml from: " + resource);
                }
	            InputStream is = new UrlResource(resource).getInputStream();

	            if (is != null) {
		            read(classLoader, is);
	            }
            }

		}
		catch (Exception e) {
			_log.error(e, e);
		}
	}

	public Map<String, String> getDefaultHints(String model) {
		return _defaultHints.get(model);
	}

	public com.liferay.portal.kernel.xml.Element getFieldsEl(
		String model, String field) {

		Map<String, Object> fields =
			(Map<String, Object>)_modelFields.get(model);

		if (fields == null) {
			return null;
		}
		else {
			Element fieldsEl = (Element)fields.get(field + _ELEMENTS_SUFFIX);

			if (fieldsEl == null) {
				return null;
			}
			else {
				return fieldsEl;
			}
		}
	}

	public List<String> getModels() {
		return ListUtil.fromCollection(_models);
	}

	public String getType(String model, String field) {
		Map<String, Object> fields =
			(Map<String, Object>)_modelFields.get(model);

		if (fields == null) {
			return null;
		}
		else {
			return (String)fields.get(field + _TYPE_SUFFIX);
		}
	}

	public Map<String, String> getHints(String model, String field) {
		Map<String, Object> fields =
			(Map<String, Object>)_modelFields.get(model);

		if (fields == null) {
			return null;
		}
		else {
			return (Map<String, String>)fields.get(field + _HINTS_SUFFIX);
		}
	}

	public void read(ClassLoader classLoader, String resourceName) throws Exception {
	    if (_log.isDebugEnabled()) {
		    _log.debug("Loading "+resourceName);
	    }
        read(classLoader, classLoader.getResourceAsStream(resourceName));
    }

	public void read(ClassLoader classLoader, InputStream is) throws Exception {
		String xml = null;

        try {
		    try {
			    xml = StringUtil.read(is);
		    }
		    catch (Exception e) {
			    if (_log.isWarnEnabled()) {
				    _log.warn("Cannot read from InputStream!");
			    }
		    }

		    if (xml == null) {
			    return;
		    }

		    if (_log.isDebugEnabled()) {
			    _log.debug("Loading OK");
		    }

		    Document doc = _saxReader.read(xml);

		    Element root = doc.getRootElement();

		    Iterator<Element> itr1 = root.elements("hint-collection").iterator();

		    while (itr1.hasNext()) {
			    Element hintCollection = itr1.next();

			    String name = hintCollection.attributeValue("name");

			    Map<String, String> hints = _hintCollections.get(name);

			    if (hints == null) {
				    hints = new HashMap<String, String>();

				    _hintCollections.put(name, hints);
			    }

			    Iterator<Element> itr2 = hintCollection.elements("hint").iterator();

			    while (itr2.hasNext()) {
				    Element hint = itr2.next();

				    String hintName = hint.attributeValue("name");
				    String hintValue = hint.getText();

				    hints.put(hintName, hintValue);
			    }
		    }

		    itr1 = root.elements("model").iterator();

		    while (itr1.hasNext()) {
			    Element model = itr1.next();

			    String name = model.attributeValue("name");

			    if (classLoader != ExtletModelHintsImpl.class.getClassLoader()) {
				    ClassNameLocalServiceUtil.getClassName(name);
			    }

			    Map<String, String> defaultHints = new HashMap<String, String>();

			    _defaultHints.put(name, defaultHints);

			    Element defaultHintsEl = model.element("default-hints");

			    if (defaultHintsEl != null) {
				    Iterator<Element> itr2 = defaultHintsEl.elements(
					    "hint").iterator();

				    while (itr2.hasNext()) {
					    Element hint = itr2.next();

					    String hintName = hint.attributeValue("name");
					    String hintValue = hint.getText();

					    defaultHints.put(hintName, hintValue);
				    }
			    }

			    Map<String, Object> fields =
				    (Map<String, Object>)_modelFields.get(name);

			    if (fields == null) {
				    fields = new HashMap<String, Object>();

				    _modelFields.put(name, fields);
			    }

			    _models.add(name);

			    Iterator<Element> itr2 = model.elements("field").iterator();

			    while (itr2.hasNext()) {
				    Element field = itr2.next();

				    String fieldName = field.attributeValue("name");
				    String fieldType = field.attributeValue("type");

				    Map<String, String> fieldHints = new HashMap<String, String>();

				    fieldHints.putAll(defaultHints);

				    Iterator<Element> itr3 = field.elements(
					    "hint-collection").iterator();

				    while (itr3.hasNext()) {
					    Element hintCollection = itr3.next();

					    Map<String, String> hints = _hintCollections.get(
						    hintCollection.attributeValue("name"));

					    fieldHints.putAll(hints);
				    }

				    itr3 = field.elements("hint").iterator();

				    while (itr3.hasNext()) {
					    Element hint = itr3.next();

					    String hintName = hint.attributeValue("name");
					    String hintValue = hint.getText();

					    fieldHints.put(hintName, hintValue);
				    }

				    fields.put(fieldName + _ELEMENTS_SUFFIX, field);
				    fields.put(fieldName + _TYPE_SUFFIX, fieldType);
				    fields.put(fieldName + _HINTS_SUFFIX, fieldHints);
			    }
		    }
        } finally {
            if(is != null){
                is.close();
            }
        }
	}

	public void setSAXReader(SAXReader saxReader) {
		_saxReader = saxReader;
	}

	public String trimString(String model, String field, String value) {
		if (value == null) {
			return value;
		}

		Map<String, String> hints = getHints(model, field);

		if (hints == null) {
			return value;
		}

		int maxLength = GetterUtil.getInteger(
			ModelHintsConstants.TEXT_MAX_LENGTH);

		maxLength = GetterUtil.getInteger(hints.get("max-length"), maxLength);

		if (value.length() > maxLength) {
			return value.substring(0, maxLength);
		}
		else {
			return value;
		}
	}

    public static final String EXTLET_MODEL_RESOURCE_NAME = "META-INF/extlet-model-hints.xml";

    private static final String _ELEMENTS_SUFFIX = "_ELEMENTS";

    private static final String _TYPE_SUFFIX = "_TYPE";

	private static final String _HINTS_SUFFIX = "_HINTS";

	private static Log _log = LogFactoryUtil.getLog(ExtletModelHintsImpl.class);

	private Map<String, Map<String, String>> _hintCollections;
	private Map<String, Map<String, String>> _defaultHints;
	private Map<String, Object> _modelFields;
	private Set<String> _models;
	private SAXReader _saxReader;

}
