<button class="btn btn-info" data-target="#alerts" data-toggle="modal" type="button">Email Alerts</button>
<div class="modal fade action-content" id="alerts" role="dialog">
	<div class="modal-dialog modal-dialog-centered" role="document">
		<form class="modal-content" ng-submit="addMailerPassword()">
			<div class="modal-header">
			    <h5 class="modal-title">Email alerts</h5>
			</div>
			<div class="modal-body">
			    <div class="section action">
			        <label class="checkbox-inline"><input ng-model="config.alerts.enabled" type="checkbox"> Enabled</label>
			    </div>
                <div class="section action">
                    <b>Sender</b><br>
                    Host
                    <input class="form-control flex-wrap" required ng-model="config.alerts.mailer.host" type="text" ng-change="modifying()">
                    Port
                    <input class="form-control flex-wrap" required ng-model="config.alerts.mailer.port" type="number" ng-change="modifying()">
                    Email Address
                    <input class="form-control flex-wrap" required ng-model="config.alerts.mailer.email" type="text" ng-change="modifying()">
                    Email Password
                    <input class="form-control flex-wrap" id="pswd" required type="password">
                </div>
                <div class="section action table-responsive">
                    <b>Receivers</b>
                    <table class="table table-striped">
                        <thead>
                            <tr>
                                <th scope="col">Delete</th>
                                <th scope="col">Role</th>
                                <th scope="col">Attach config</th>
                                <th scope="col">Days logs to attach</th>
                                <th scope="col">Emails separated by #</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td></td>
                                <td>
                                    <select class="form-control multiselect-ui" ng-model="alert.eventCategory">
										<option value="ADMIN">Administrator</option>
										<option value="BROADCAST">Broadcaster</option>
									</select>
                                </td>
                                <td>
                                    <input ng-model="alert.attachConfig" type="checkbox" ng-disabled="isEmpty(alert.eventCategory) || alert.eventCategory != 'ADMIN'">
                                </td>
                                <td>
                                    <input ng-model="alert.attachAuditLog" type="number" ng-disabled="isEmpty(alert.eventCategory) || alert.eventCategory != 'ADMIN'">
                                </td>
                                <td>
                                    <input ng-model="alert.emails" type="text">
                                </td>
                            </tr>
                            <tr ng-repeat="(k, s) in config.alerts.subscribers">
                                <td>
                                    <input class="receiver" value="{{k}}" type="checkbox">
                                </td>
                                <td>{{s.eventCategory}}</td>
                                <td>{{s.attachConfig}}</td>
                                <td>{{s.attachAuditLog}}</td>
                                <td>{{s.emails}}</td>
                            </tr>
                        </tbody>
                    </table>
                </div>
			</div>
			<div class="modal-footer">
				<button class="btn btn-danger" id="close-alert" data-dismiss="modal"	ng-click="clear()" type="button">Cancel</button>
				<button class="btn btn-danger" type="button" ng-disabled="isEmpty(config.alerts.subscribers)" ng-click="deleteReceivers()">Delete Selected Receivers</button>
                <button class="btn btn-success"  type="button" ng-disabled="isEmpty(alert.emails) || isEmpty(alert.attachConfig) || isEmpty(alert.attachAuditLog) || isEmpty(alert.eventCategory) || invalidSubScriber()" ng-click="addAlert()">Add Receiver</button>
				<button class="btn btn-success" type="submit" ng-disabled="isEmpty(config.alerts.subscribers)" >Save</button>
			</div>
		</form>
	</div>
</div>