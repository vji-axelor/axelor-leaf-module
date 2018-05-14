/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2018 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.apps.base.web;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.apps.base.db.Wizard;
import com.axelor.apps.base.exceptions.IExceptionMessage;
import com.axelor.apps.base.service.DuplicateObjectsService;
import com.axelor.common.Inflector;
import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.i18n.I18n;
import com.axelor.meta.db.MetaField;
import com.axelor.meta.db.MetaModel;
import com.axelor.meta.db.repo.MetaFieldRepository;
import com.axelor.meta.db.repo.MetaModelRepository;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Context;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mysql.jdbc.StringUtils;
import com.axelor.rpc.filter.Filter;


@Singleton
public class DuplicateObjectsController {
	
	private static final Logger LOG = LoggerFactory.getLogger( MethodHandles.lookup().lookupClass() );
	
	@Inject
	private DuplicateObjectsService duplicateObjectService;
	
	@Inject
	private MetaFieldRepository metaFieldRepo;
	
	@Inject
	private MetaModelRepository metaModelRepo;

	public void removeDuplicate(ActionRequest request, ActionResponse response) {
		List<Long> selectedIds = new ArrayList<>();
		String originalId = ((Map)request.getContext().get("originalObject")).get("recordId").toString();
		selectedIds.add(Long.parseLong(originalId));
		List<Map<String,Object>> duplicateObjects = (List<Map<String, Object>>) request.getContext().get("duplicateObjects");
		for(Map map : duplicateObjects) {
			if(!map.get("recordId").toString().equals(originalId)) {
				selectedIds.add(Long.parseLong(map.get("recordId").toString()));
			}
		}
		String model = request.getContext().get("_modelName").toString();
		String modelName = model.substring(model.lastIndexOf(".")+1, model.length());
		duplicateObjectService.removeDuplicate(selectedIds, modelName);
		response.setCanClose(true);
	}
	
	public void defaultObjects(ActionRequest request, ActionResponse response) throws NoSuchFieldException, SecurityException {
		List<Long> selectedIds = new ArrayList<>();
		List<Object[]> duplicateObjects = new ArrayList<>();
		List<Wizard> wizardDataList = new ArrayList<>();
		for (Integer id : (List<Integer>) request.getContext().get("_ids")) {
			selectedIds.add(Long.parseLong("" + id));
		}
		String modelName = request.getContext().get("_modelName").toString();
		List<Object> duplicateObj= duplicateObjectService.getAllSelectedObject(selectedIds, modelName);
		
		for(Object object : duplicateObj) {
			Long id = (Long) Mapper.of(object.getClass()).get(object, "id"); 
			Property propertyNameColumn = Mapper.of(object.getClass()).getNameField();
			String nameColumn = propertyNameColumn == null ? null : propertyNameColumn.getName().toString();
			Property propertyCode = Mapper.of(object.getClass()).getProperty("code");
			String code = propertyCode == null ? null : propertyCode.getName().toString();
			String noColumn = null;
			if(nameColumn != null) {
				duplicateObjects.add((Object[]) duplicateObjectService.getWizardValue(id, modelName, nameColumn));
			} else if(code != null) {
				duplicateObjects.add((Object[]) duplicateObjectService.getWizardValue(id, modelName, code));	
			} else {
				Object obj = duplicateObjectService.getWizardValue(id, modelName, noColumn);
				Wizard wizard = new Wizard();
				wizard.setRecordId(obj.toString());
				wizard.setName(obj.toString());
				wizardDataList.add(wizard);
			}
		}
		for(Object[] obj : duplicateObjects) {
			String recordName = obj[1].toString();
			String recordId = obj[0].toString();
			Wizard wizard = new Wizard();
			wizard.setRecordId(recordId);
			wizard.setRecordName(recordName);
			wizard.setName(recordName);
			wizardDataList.add(wizard);
			
		}
		
		response.setAttr("$duplicateObjects", "value", wizardDataList);
		
	}
	
	public void addOriginal(ActionRequest request, ActionResponse response) {
		Context context = request.getContext();
		List<Map<String, Object>> duplicateObj = (List<Map<String, Object>>) context.get("duplicateObjects");
		Object originalObj = null;
		Object original = "";
		boolean flag = false;
		for (Map map : duplicateObj) {
			if ((boolean) map.get("selected")) {
				originalObj = context.get("originalObject");
				response.setAttr("$originalObject", "value", map);
				original = map;
				flag = true;
			} 
		}
		if(!flag) {
			response.setAlert("Please select original object");
  		}
		duplicateObj.remove(original);
		if(originalObj != null) {
			duplicateObj.add((Map<String, Object>) originalObj);
		}
		
		response.setAttr("$duplicateObjects", "value", duplicateObj);
	}
	
	/**
	 * show duplicate records
	 * 
	 * @param request
	 * @param response
	 */
	@SuppressWarnings("unchecked")
	public void showDuplicate(ActionRequest request, ActionResponse response) {

		String model = (String) request.getContext().get("object");
		List<String> fields = new ArrayList<String>();

		if (model == null) {
			model = request.getModel();
			String searchFields = (String) request.getContext().get("searchFields");
			if (searchFields != null) {
				fields.addAll(Arrays.asList(searchFields.split(";")));
			}
		} else {

			if (request.getContext().get("fieldsSet") != null) {
				List<HashMap<String, Object>> fieldsSet = (List<HashMap<String, Object>>) request.getContext()
						.get("fieldsSet");

				for (HashMap<String, Object> field : fieldsSet) {
					MetaField metaField = metaFieldRepo.find(Long.parseLong(field.get("id").toString()));
					fields.add(metaField.getName());
				}
			}
		}

		LOG.debug("Duplicate record model: {}", model);

		if (fields.size() > 0) {

			LOG.debug("Duplicate record joinList: {}", fields);

			String criteria = (String) (request.getContext().get("_criteria"));

			if (criteria == null) {
				if (request.getContext().get("_ids") != null) {
					criteria = request.getContext().get("_ids").toString();
				} else {
					criteria = this.getCriteria(request);
				}
			}

			if (criteria.equals("[]")) {
				response.setError(I18n.get(IExceptionMessage.GENERAL_8));
			} else {
				criteria = criteria.substring(1, criteria.length() - 1);
				String ids = duplicateObjectService.findDuplicateRecords(fields, model, criteria);
				if (ids.isEmpty())
					response.setFlash(I18n.get(IExceptionMessage.GENERAL_1));
				else {
					String domain = "self.id in (" + ids + ")";
					Class<?> modelClass = JPA.model(model);
					final Inflector inflector = Inflector.getInstance();
					String viewName = inflector.dasherize(modelClass.getSimpleName());

					response.setView(ActionView.define(I18n.get(IExceptionMessage.GENERAL_2))
							.model(model)
							.add("grid", String.format("%s-grid", viewName))
							.add("form", String.format("%s-form", viewName))
							.domain(domain).context("_domain", domain)
							.map());

					response.setCanClose(true);
				}
			}
		} else
			response.setFlash(I18n.get(IExceptionMessage.GENERAL_3));
	}

	/**
	 * call check duplicate wizard
	 * 
	 * @param request
	 * @param response
	 */
	@SuppressWarnings("unchecked")
	public void callCheckDuplicateWizard(ActionRequest request, ActionResponse response) {

		LOG.debug("Call check duplicate wizard for model : {} ", request.getModel());

		MetaModel metaModel = metaModelRepo.all().filter("self.fullName = ?", request.getModel()).fetchOne();
		String criteria;

		if (request.getContext().get("_ids") != null) {
			criteria = request.getContext().get("_ids").toString();
		} else {
			criteria = this.getCriteria(request);
		}
		if (criteria.equals("[]")) {
			response.setError(I18n.get(IExceptionMessage.GENERAL_8));
		} else {
			response.setView(ActionView.define("Check duplicate")
					.model(Wizard.class.getName())
					.add("form", "wizard-check-duplicate-form")
					.param("popup", "true").param("show-toolbar", "false")
					.param("width", "500")
					.param("popup-save", "false")
					.context("object", metaModel.getFullName())
					.context("_criteria", criteria).map());
		}
	}

	private String getCriteria(ActionRequest request) {
		
		MetaModel metaModel = metaModelRepo.all().filter("self.fullName = ?", request.getModel()).fetchOne();
		Class<? extends Model> klass = (Class<? extends Model>) request.getBeanClass();
		Filter filter = duplicateObjectService.getJpaSecurityFilter(metaModel);		
		Stream<? extends Model> listObj = request.getCriteria().createQuery(klass, filter).fetchSteam();
        
		return listObj.map(it->it.getId()).collect(Collectors.toList()).toString();
	}

}
