<!DOCTYPE html>
<html>

<head>
	<link href="favicon.ico" rel="icon" type="image/png">
	<link href="res/bootstrap.min.css" rel="stylesheet">
	<script src="res/jquery.min.js"></script>
	<script src="res/popper.min.js"></script>
	<script src="res/bootstrap.min.js"></script>
	<meta charset="utf-8">
	<meta content="width=device-width, initial-scale=1" name="viewport">
	<script src="res/angular.min.js"></script>
	<script src="res/angular-cookies.js"></script>
	<script src="res/bootstrap-datepicker.js"></script>
	<link href="res/datepicker.css" rel="stylesheet">
	<link href="res/jquery.timepicker.min.css" rel="stylesheet">
	<script src="res/jquery.timepicker.min.js"></script>
	<script src="res/jQuery.print.js"></script>
	<link href="res/bootstrap-colorselector.min.css" rel="stylesheet">
	<script src="res/bootstrap-colorselector.min.js"></script>
	<script src="res/.js"></script>
	<link href="res/telefyna.css" rel="stylesheet">
	<script src="res/telefyna.js"></script>
	<title>Configuring Telefyna</title>
</head>

<body center-block ng-app="Telefyna" ng-controller="Config">
	<div>
		<div>
			<h6>|Configuring|</h6>
			<img alt="Telefyna Icon" class="logo" height="50" src="telefyna.png" width="55">
			<br>
			<label class="slogan">The best, simplest performing online stream & local file scheduling auto player for TV broadcasting</label>
		</div>
		<!-- Other configurations before playlists-->
		<div class="section">
			<label>Last Modified: <b>{{config.lastModified}}</b>
			</label>
			<div class="section action">Name
				<input class="form-control flex-wrap" ng-model="config.name" ng-change="modifying()" type="text">
			</div>
			<div class="section action">Version
				<input class="form-control flex-wrap" ng-model="config.version" ng-change="modifying()" type="text">
			</div>
			<div class="section action">Pinging time (seconds): time to keep checking the player and wait for internet
				<input class="form-control flex-wrap" ng-model="config.wait" ng-change="modifying()" type="number">
			</div>
			<div class="form-check form-switch">
				<label class="checkbox-inline"><input ng-model="config.automationDisabled" ng-change="modifying()" type="checkbox"> Disable Automation</label>
			</div>
			<div class="form-check form-switch">
				<label class="checkbox-inline"><input ng-model="config.notificationsDisabled" ng-change="modifying()" type="checkbox"> Disable OS Notifications</label>
			</div>
			<div class="action">
				<!-- Create -->
				<button class="btn btn-info" data-target="#add" data-toggle="modal" type="button">Create Playlist</button>
				<div class="modal fade action-content" id="add" role="dialog">
					<div class="modal-dialog modal-dialog-centered" role="document">
						<form class="modal-content" ng-submit="add()">
							<div class="modal-header">
								<h5 class="modal-title">Create a new Playlist</h5>
							</div>
							<div class="modal-body">
								<div ng-if="isEmpty(config.playlists)">This is your first and Default playlist, It's played when nothing is scheduled without <b>emptyReplacer</b> or programs are unexisting in the folder and when automation is disabled.</div>
								<div ng-if="config.playlists.length == 1">This is your second and Fillers playlist, It's played when programs finish before the schedule/slot is ended and when internet breaks or is unavailable</div>
								<?php include 'playlist.php';?>
								<?php include 'graphics.php';?>
							</div>
							<div class="modal-footer">
								<button class="btn btn-info" ng-click="clear()" type="button">Clear</button>
								<button class="btn btn-danger"  type="button" ng-disabled="isEmpty(playlist.graphics.lowerThirds)" ng-click="deleteLowerThirds()">Delete Selected Lower Thirds</button>
								<button class="btn btn-success"  type="button" ng-disabled="isEmpty(lowerThird.replays) || isEmpty(lowerThird.file) || isEmpty(lowerThird.starts)" ng-click="addLowerThird()">Add Lower Third</button>
								<button class="btn btn-danger"  type="button" data-dismiss="modal" id="close-add" ng-click="clear()">Cancel</button>
								<button class="btn btn-success" type="submit" ng-disabled="isEmpty(playlist.name) || isEmpty(playlist.type) || isEmpty(playlist.urlOrFolder)">Add</button>
							</div>
						</form>
					</div>
				</div>
				<!-- Edit -->
				<button class="btn btn-info" data-target="#edit" data-toggle="modal" ng-disabled="isEmpty(config.playlists)" type="button">Edit Playlist</button>
				<div class="modal fade action-content" id="edit" role="dialog">
					<div class="modal-dialog modal-dialog-centered" role="document">
						<form class="modal-content" ng-submit="revise()">
							<div class="modal-header">
								<h5 class="modal-title">Edit an existing Playlist</h5>
							</div>
							<div class="modal-body">
								<div ng-if="edit == 0">This is your first and Default playlist, It's played when nothing is scheduled  without <b>emptyReplacer</b> or programs are not existing in the folder and when automation is disabled.</div>
								<div ng-if="edit == 1">This is your second and Fillers playlist, It's played when programs finish before the schedule/slot is ended and when internet breaks or is unavailable.</div>
								<div class="section action">* Select Playlist to edit *
									<select class="form-control" ng-change="renderEdit()" ng-model="edit" required>
										<option ng-repeat="(k, p) in config.playlists track by k" ng-if="isNotScheduled(p)" value="{{k}}">{{getPlaylistName(k, true)}}</option>
									</select>
								</div>
								<?php include 'playlist.php';?>
								<?php include 'graphics.php';?>
							</div>
							<div class="modal-footer">
								<button class="btn btn-danger" data-dismiss="modal" id="close-edit" ng-click="clear()" type="button">Cancel</button>
								<button class="btn btn-danger"  type="button" ng-disabled="isEmpty(playlist.graphics.lowerThirds)" ng-click="deleteLowerThirds()">Delete Selected Lower Thirds</button>
								<button class="btn btn-success"  type="button" ng-disabled="isEmpty(lowerThird.replays) || isEmpty(lowerThird.file) || isEmpty(lowerThird.starts)" ng-click="addLowerThird()">Add Lower Third</button>
								<button class="btn btn-success" type="submit" ng-disabled="isEmpty(edit) || isEmpty(playlist.name) || isEmpty(playlist.type) || isEmpty(playlist.urlOrFolder)">Save</button>
							</div>
						</form>
					</div>
				</div>
				<!-- Schedule/Copy -->
				<button class="btn btn-info" data-target="#schedule" data-toggle="modal" ng-disabled="isEmpty(config.playlists)" type="button">Scheduling</button>
				<div class="modal fade action-content" id="schedule" role="dialog">
					<div class="modal-dialog modal-dialog-centered" role="document">
						<form class="modal-content" ng-submit="scheduling()">
							<div class="modal-header">
								<h5 class="modal-title">Schedule an existing Playlist/schedule</h5>
							</div>
							<div class="alert alert-danger section action" ng-show="!isEmpty(error)" role="alert">{{error}}</div>
							<div class="modal-body">
								<div class="section action">* Select Playlist/Schedule *
									<select class="form-control" ng-model="schedule" required ng-change="renderScheduling()">
										<option ng-repeat="(k, p) in config.playlists track by k" value="{{k}}">{{getPlaylistName(k, true)}}</option>
									</select>
								</div>
								<div class="section action">
									<label class="checkbox-inline">
										<input ng-model="playlist.active" type="checkbox"> Active</label>
								</div>
								<div class="section action">Weekly Day(s)
									<select class="form-control multiselect-ui" multiple ng-model="playlist.days">
										<option value="1">{{getDayName(1)}}</option>
										<option value="2">{{getDayName(2)}}</option>
										<option value="3">{{getDayName(3)}}</option>
										<option value="4">{{getDayName(4)}}</option>
										<option value="5">{{getDayName(5)}}</option>
										<option value="6">{{getDayName(6)}}</option>
										<option value="7">{{getDayName(7)}}</option>
									</select>
								</div>
								<div class="section action">Date(s)
									<input class="form-control date" ng-model="datePickerValue" ng-change="setPlaylistDate()" type="text" id="dates">
								</div>If no Days or Dates are selected but start Time is defined, the playlist schedules daily
								<div class="section action">* Start Time *
									<input class="form-control timepicker" ng-model="playlist.start" required type="text" placeholder="xx:xx">
								</div>
								<div class="section action">
									Override Graphics
									<?php include 'graphics.php';?>
                                </div>
							</div>
							<div class="modal-footer">
							<button class="btn btn-danger" type="button" ng-click="deleteAllSchedules()">Delete All Schedules</button>
								<button class="btn btn-info" type="button" ng-click="clear()" type="button">Clear</button>
								<button class="btn btn-danger" type="button" ng-disabled="isEmpty(playlist.graphics.lowerThirds)" ng-click="deleteLowerThirds()">Delete Selected Lower Thirds</button>
								<button class="btn btn-success" type="button" ng-disabled="isEmpty(lowerThird.replays) || isEmpty(lowerThird.file) || isEmpty(lowerThird.starts)" ng-click="addLowerThird()">Add Lower Third</button>
								<button class="btn btn-danger" type="button" data-dismiss="modal" id="close-schedule" ng-click="clear()">Cancel</button>
								<button class="btn btn-success" type="submit" ng-disabled="isEmpty(schedule) || isEmpty(playlist.start)">Schedule</button>
							</div>
						</form>
					</div>
				</div>
				<!-- Alerts -->
				<?php include 'alert.php';?>
			</div>
		</div>
		<div class="section">
			<!-- Clear/Delete -->
			<div class="action">
				<button class="btn btn-danger" ng-click="clearConfig()" ng-disabled="isEmpty(config.playlists)" type="button">Clear</button>
				<button class="btn btn-danger" data-target="#delete" data-toggle="modal" ng-disabled="isEmpty(config.playlists)" type="button">Delete Playlist(s) | Schedule(s)</button>
				<div class="modal fade action-content" id="delete" role="dialog">
					<div class="modal-dialog modal-dialog-centered" role="document">
						<form class="modal-content" ng-submit="delete()">
							<div class="modal-header">
								<h5 class="modal-title">Delete Existing Playlist(s) | Schedule(s)</h5>
							</div>Deleting the playlist will delete respective Schedules below it whereas deleting schedule doesn't delete the respective playlist
							<div class="modal-body">
								<div class="section action">Select Playlists(s) | Schedule(s)
									<select class="form-control multiselect-ui" id="playlists-delete" multiple ng-model="deletable">
										<option ng-repeat="(k, p) in config.playlists track by k" value="{{k}}">{{getPlaylistName(k, true)}}</option>
									</select>
								</div>
							</div>
							<div class="modal-footer">
								<button class="btn btn-info" ng-click="deletable = []" type="button">Clear</button>
								<button class="btn btn-danger" data-dismiss="modal" id="close-delete" ng-click="clear()" type="button">Cancel</button>
								<button class="btn btn-danger" ng-disabled="isEmpty(deletable)" type="submit">Delete Selected</button>
							</div>
						</form>
					</div>
				</div>
			</div>
			<!-- Demo/Import/Export -->
			<div class="action">
				<button class="btn btn-info" data-target="#demo" data-toggle="modal" type="button">Demo</button>
				<div class="modal fade action-content" id="demo" role="dialog">
					<div class="modal-dialog modal-dialog-centered" role="document">
						<div class="modal-content">
							<div class="modal-header">
								<h5 class="modal-title">Telefyna Configuration Demonstration</h5>
							</div>
							<div class="modal-body">
								<h5><a allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen frameborder="0" height="315" href="https://www.youtube.com/embed/Oy5aN6MTcXM" target="_blank">Youtube configuration demo</a></h5>
								<br>
								<h5>Telefyna Infrastructure</h5>
								<img alt="Telefyna Insfrastructure" src="telefynaDesign.png">
								<br>
							</div>
							<div class="modal-footer">
								<button class="btn btn-danger" data-dismiss="modal" id="close-delete" type="button">Close</button>
							</div>
						</div>
					</div>
				</div>
				<button class="btn btn-info" onclick="jQuery('#import-config').click()" type="file">Import</button>
				<input hidden id="import-config" ng-model="configFile" onchange="angular.element(this).scope().importConfig(event)" type="file">
				<!-- Preview -->
				<button class="btn btn-success" data-target="#preview" data-toggle="modal" ng-click="initPreviewData()" ng-disabled="isEmpty(config.playlists)" type="button">Preview Schedule</button>
				<div class="modal fade action-content" id="preview" role="dialog">
					<div class="modal-dialog modal-dialog-centered" role="document">
						<div class="modal-content preview-print">
							<div class="alert-warning" ng-if="config.automationDisabled == true">Automation is disabled and only: "{{config.playlists[0].name}}" will be playing all through</div>
							<div class="modal-header" id="print-title">
								<img alt="Telefyna Icon" height="50" src="telefyna.png" width="55">
								<br>
								<h4>{{config.name}}'s schedule</h4>
								Version: {{config.version}}
							</div>
							<!--.table-responsive breaks print-->
							<div class="modal-body">
								<div ng-if="!isEmpty(previewData.weekly)" class="table-responsive">
									<h5>Weekly</h5>
									<table class="table table-striped">
										<thead>
											<tr>
												<th scope="col">StartTime</th>
												<th scope="col">Sunday</th>
												<th scope="col">Monday</th>
												<th scope="col">Tuesday</th>
												<th scope="col">Wednesday</th>
												<th scope="col">Thursday</th>
												<th scope="col">Friday</th>
												<th scope="col">Saturday</th>
											</tr>
										</thead>
										<tbody>
											<tr ng-repeat="(key, program) in previewData.weekly">
												<td>{{program.start}}</td>
												<td class="{{program.slots[1].claz}}" ng-init="classifyColor(program.slots[1].color, key, 1)">{{program.slots[1].name}}</td>
												<td class="{{program.slots[2].claz}}" ng-init="classifyColor(program.slots[2].color, key, 2)">{{program.slots[2].name}}</td>
												<td class="{{program.slots[3].claz}}" ng-init="classifyColor(program.slots[3].color, key, 3)">{{program.slots[3].name}}</td>
												<td class="{{program.slots[4].claz}}" ng-init="classifyColor(program.slots[4].color, key, 4)">{{program.slots[4].name}}</td>
												<td class="{{program.slots[5].claz}}" ng-init="classifyColor(program.slots[5].color, key, 5)">{{program.slots[5].name}}</td>
												<td class="{{program.slots[6].claz}}" ng-init="classifyColor(program.slots[6].color, key, 6)">{{program.slots[6].name}}</td>
												<td class="{{program.slots[7].claz}}" ng-init="classifyColor(program.slots[7].color, key, 7)">{{program.slots[7].name}}</td>
											</tr>
										</tbody>
									</table>
								</div>
								<div ng-if="!isEmpty(previewData.dated)" class="table-responsive">
									<h5>Dates</h5>
									<table class="table table-striped">
										<thead>
											<tr>
												<th scope="col">Time</th>
												<th scope="col">Playlist</th>
											</tr>
										</thead>
										<tbody>
											<tr ng-repeat="program in previewData.dated">
												<td>{{program.at}}</td>
												<td style="background-color:{{program.color}}">{{program.name}}</td>
											</tr>
										</tbody>
									</table>
								</div>
							</div>
							<div class="modal-footer no-print">
								<label>This preview only includes active schedules</label>
								<button class="btn btn-success" onclick="jQuery('.preview-print').print({globalStyles: true, stylesheet: 'res/telefyna.css'})" type="button">Print</button>
								<button class="btn btn-danger" data-dismiss="modal" id="close-preview" type="button">Close</button>
							</div>
						</div>
					</div>
				</div>
				<button class="btn btn-success" ng-click="exportConfig()" ng-disabled="isEmpty(config.playlists)" type="button">Export</button>
			</div>
		</div>
		<div class="info alert-info"> <b>telefyna</b> folder should be put in either/both Internal/SDcard whereas <b>telefynaAudit</b> exists in Internal storage
			<br><b>The export(config.json) contains sensitive information and should be protected, <br>Telefyna runs the scheduling at midnight daily;</b>
			<br>This means if a program isn't in the folder before midnight it won't be scheduled or changes to config thereafter won't be picked up. To order Telefyna to re-run scheduling and use your changes afer the current program, add a file named <b>init.txt</b> in your <b>telefynaAudit</b> folder. Add a file named <b>restart.txt|reboot.txt</b> in your <b>telefynaAudit</b> to restart Telefyna or reboot device respectively after <b>wait</b>. Use <b>backupConfig.txt|backupConfigReset.txt</b> to regenerate copy of <b>config.json</b> in <b>telefynaAudit</b> at next <b>wait</b>. Put your program folders in <b>telefyna/playlist</b> Put your <b>urlOrFolder-INTRO</b> and <b>urlOrFolder-OUTRO</b>, special(anything absolute reference folder) and <b>General</b> bumper folders in <b>telefyna/bumper</b>.(bumpers are played in playlist, special and general bumper order before the programs), lower thirds in <b>telefyna/lowerThird</b>, And your logo (not more than 200KB) at <b>telefyna/logo.png</b>
		</div>
		<!-- Display Results -->
		<!--div class="section config-result">
            <pre>{{config | json: 2}}</pre>
        </div-->
		<a hidden id="export"></a>
		<div id="footer">
            apps[@]avventohome[.]org | <label>Copyright ?? 2021</label>
		</div>
	</div>
</body>
</html>