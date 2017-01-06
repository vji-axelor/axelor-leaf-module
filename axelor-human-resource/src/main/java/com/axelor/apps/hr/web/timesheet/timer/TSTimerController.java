/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2016 Axelor (<http://axelor.com>).
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
package com.axelor.apps.hr.web.timesheet.timer;

import com.axelor.apps.hr.db.TSTimer;
import com.axelor.apps.hr.db.repo.TSTimerRepository;
import com.axelor.apps.hr.service.timesheet.timer.TimesheetTimerService;
import com.axelor.i18n.I18n;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Inject;

public class TSTimerController{
	
	@Inject
	protected TSTimerRepository TsTimerRepo;
	
	@Inject
	protected TimesheetTimerService tsTimerService;
	
	public void editTimesheetTimer(ActionRequest request, ActionResponse response){
		TSTimer tsTimer = tsTimerService.getCurrentTSTimer();
		if(tsTimer == null){
			response.setView(ActionView
									.define(I18n.get("TSTimer"))
									.model(TSTimer.class.getName())
									.add("form", "ts-timer-form")
									.map());
		}
		else{
			response.setView(ActionView
					.define(I18n.get("TSTimer"))
					.model(TSTimer.class.getName())
					.add("form", "ts-timer-form")
					.param("forceEdit", "true")
					.context("_showRecord", String.valueOf(tsTimer.getId())).map());
		}
	}
	
	public void editTimesheetTimerFromTimesheet(ActionRequest request, ActionResponse response){
		TSTimer tsTimer = tsTimerService.getCurrentTSTimer();
		if(tsTimer == null){
			response.setView(ActionView
									.define(I18n.get("TSTimer"))
									.model(TSTimer.class.getName())
									.add("form", "ts-timer-form")
									.param("popup", "reload")
									.param("forceEdit", "true")
									.param("width", "800")
									.param("show-confirm", "true")
									.param("show-toolbar", "false")
									.param("popup-save", "true")
									.map());
		}
		else{
			response.setView(ActionView
					.define(I18n.get("TSTimer"))
					.model(TSTimer.class.getName())
					.add("form", "ts-timer-form")
					.param("popup", "reload")
					.param("forceEdit", "true")
					.param("width", "800")
					.param("show-confirm", "true")
					.param("show-toolbar", "false")
					.param("popup-save", "true")
					.context("_showRecord", String.valueOf(tsTimer.getId())).map());
		}
	}
	
	public void pause(ActionRequest request, ActionResponse response){
		TSTimer timerView = request.getContext().asType(TSTimer.class);
		TSTimer timer = TsTimerRepo.find(timerView.getId());
		
		tsTimerService.pause(timer);
		
		response.setReload(true);
	}
	
	public void stop(ActionRequest request, ActionResponse response){
		TSTimer timerView = request.getContext().asType(TSTimer.class);
		TSTimer timer = TsTimerRepo.find(timerView.getId());
		
		tsTimerService.stop(timer);
		
		if(timer.getDuration() < 60)
			response.setFlash(I18n.get("No timesheet line has been created because the duration is less than 1 minute"));
		response.setReload(true);
	}
	
}