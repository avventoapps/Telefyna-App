jQuery(function() {
    jQuery('.date').datepicker({
        multidate: true,
        format: 'dd-mm-yyyy'
    });
    jQuery(".timepicker").timepicker();
});

angular.module("Telefyna", ['ngCookies']).controller('Config', function ($cookies, $scope) {
    /* TODO fix cookies
    if(!$cookies || !$cookies.getObject("config")) {
        
    }*/
    $scope.config = {};
    $scope.config.playlists = [];
    $scope.playlist = {};
    $scope.error;
    $scope.datePickerValue;
    $scope.edit;
    $scope.deletable = [];

    $scope.importConfig = function(event) {
        var file = event.target.files[0];
        if(file.name = "config.json" && file.type == "application/json") {
            const reader = new FileReader()
            reader.onload = (e) => {
                $scope.config = JSON.parse(reader.result);
                // TODO investigate why next line adds name attribute to clone playlists
                $scope.$apply();
            }
            reader.readAsText(file);
        } else {
            alert("Please select a right configuration file!");
        }
    }

    $scope.name = function(index) {
        var name;
        var playlist = $scope.config.playlists[index];
        if(playlist) {
            if(!$scope.isNotClone(playlist)) {
                playlist.name = $scope.config.playlists[playlist.clone].name;
            }
            name = playlist.name + " #" + index 
                + (playlist.start ? " | @" + playlist.start : "")
                + (playlist.days && playlist.days.length > 0 ? " | Days:" + playlist.days.join(",") : "")
                + (playlist.dates && playlist.dates.length > 0 ? " | " + playlist.dates.join(",") : "");
        }
        return name;
    }

    $scope.isNotClone = function(playlist) {
        return playlist.clone == undefined;
    }

    $scope.verifyPlaylist = function() {
        angular.forEach($scope.config.playlists, function (playlist, key) { 
            if($scope.playlist.name == playlist.name && $scope.playlist.urlOrFolder == playlist.urlOrFolder) {
                $scope.error = "Playlist Exisits";
            }
        });
    }

    $scope.add = function() {
        $scope.modify();
        $scope.verifyPlaylist();
        if(!$scope.error) {
            $scope.config.playlists.push($scope.playlist);
            jQuery("#close-add").click();
            $scope.clear();
        } else {
            jQuery("#add").scrollTop(0);
        }
    }

    $scope.renderEdit = function() {
        $scope.playlist = $scope.config.playlists[$scope.edit];
        if($scope.playlist.dates) {
            $scope.datePickerValue =$scope.playlist.dates.join(",");
        }
    }

    $scope.revise = function() {
        $scope.modify();
        $scope.config.playlists[$scope.edit] = $scope.playlist;
        jQuery("#close-edit").click();
        $scope.clear();
    }

    $scope.clone = function() {
        $scope.modify();
        $scope.config.playlists.push($scope.playlist);
        jQuery("#close-clone").click();
        $scope.clear();
    }

    $scope.delete = function() {
        if($scope.deletable.length > 0) {
            $scope.modify();
            angular.forEach($scope.deletable, function(i, key1) {
                var playlist = $scope.config.playlists[i];
                // remove clones
                angular.forEach($scope.config.playlists, function(p, key2) {
                    if(!$scope.isNotClone(p) && i == p.clone) {
                        delete $scope.config.playlists[key2];
                    }
                });
                delete $scope.config.playlists[key1];
            });
            $scope.config.playlists = $scope.config.playlists.filter(function (el) {
                return el;
            });
            jQuery("#close-delete").click();
            $scope.clear();
        }
    }

    $scope.exportConfig = function() {
        var configJson = document.getElementById("export");
        configJson.setAttribute('href', 'data:application/json;charset=utf-8,' + encodeURIComponent(angular.toJson($scope.config, 2)));
        configJson.setAttribute('download', "config.json");
        configJson.click();
        // TODO cache, export out to downloads
    }

    $scope.modify = function() {
        $scope.config.lastModified = new Date().toLocaleString();
        $scope.error = undefined;  
        $scope.edit = undefined;
        /* TODO fix local storage: CACHE THE MOST RECENT DEVICE CONFIG
            var expireDate = new Date(); 
            expireDate.setDate(expireDate.getDate() + 365 * 3); // keep for atleast 3 years
            $cookies.putObject('config', $scope.config, {expires: expireDate});
            */
    }

    $scope.clear = function() {
        $scope.playlist = {};
        $scope.error = undefined;
        $scope.datePickerValue = undefined;
        $scope.edit = undefined;
        $scope.deletable = [];
    }

    $scope.setPlaylistDate = function() {
        if($scope.datePickerValue) {
            $scope.playlist.dates = [];
            angular.forEach($scope.datePickerValue.split(","), function (d, key) { 
                $scope.playlist.dates.push(d);
            });
        }
    }

});