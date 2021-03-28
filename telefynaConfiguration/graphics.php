<div class="section action">
	<label class="checkbox-inline">
		<input ng-model="playlist.graphics.displayLogo" type="checkbox">Display Logo(telefyna/logo.png)</label>
	<div>Logo Position
		<select ng-model="playlist.graphics.logoPosition">
			<option value="TOP">Top Right</option>
			<option value="BOTTOM">Bottom Right</option>
		</select>
	</div>
	<br>
	<div class="section action">Ticker News/Notifications Separated by #
		<br>
		<textarea class="form-control" ng-model="playlist.graphics.news.messages" placeholder="Messages separed by #"></textarea>Starting minutes Separated by #
		<input class="form-control" ng-model="playlist.graphics.news.starts" type="text" placeholder="When (nth minute after start) separated by #">
		<div>Replays
			<input ng-model="playlist.graphics.news.replays" type="number">
		</div>
	</div>
	<div class="section action table-responsive">Lower thirds
		<table class="table table-striped">
			<thead>
				<tr>
					<th scope="col">Delete</th>
					<th scope="col">Replays</th>
					<th scope="col">File</th>
					<th scope="col">Starts separated by #</th>
				</tr>
			</thead>
			<tbody>
				<tr>
					<td></td>
					<td>
						<input ng-model="lowerThird.replays" type="number">
					</td>
					<td>
						<input ng-model="lowerThird.file" type="text">
					</td>
					<td>
						<input ng-model="lowerThird.starts" type="text">
					</td>
				</tr>
				<tr ng-repeat="(k, l) in playlist.graphics.lowerThirds">
					<td>
						<input class="lower-third-action" value="{{k}}" type="checkbox">
					</td>
					<td>{{l.replays}}</td>
					<td>{{l.file}}</td>
					<td>{{l.starts}}</td>
				</tr>
			</tbody>
		</table>
	</div>
</div>