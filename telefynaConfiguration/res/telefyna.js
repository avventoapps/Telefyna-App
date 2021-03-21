jQuery(function() {
    jQuery('.date').datepicker({
        multidate: true,
        format: 'dd-mm-yyyy',
        todayHighlight: true,
        startDate: new Date()
    });
    jQuery('.timepicker').timepicker({ 'timeFormat': 'H:i' });
    jQuery('.color-selector').colorselector();
});

angular.module("Telefyna", ['ngCookies']).controller('Config', function($cookies, $scope) {
    var days = [1, 2, 3, 4, 5, 6, 7];

    if(!isEmptyInternal(window.localStorage.config)) {
        $scope.config = JSON.parse(window.localStorage.config);
    } else {
        clearConfigInternal();
    }
    clearInternal();

    function isEmptyInternal(obj) {
        return obj == undefined || obj == null || obj == NaN || obj == "undefined" || obj == "null" || obj["length"] == 0;
    }

    function clearAlerts() {
        $scope.config.alerts = {};
        $scope.config.alerts.emailer = {};
        $scope.config.alerts.emailer.host = "smtp.gmail.com";
        $scope.config.alerts.emailer.port = "587";
        $scope.config.alerts.subscribers = [];
    }

    function clearConfigInternal() {
        $scope.config = {};
        $scope.config.automationDisabled = false;
        $scope.config.notificationsDisabled = true;
        $scope.config.internetWait = 60;
        clearAlerts();
        $scope.config.playlists = [];
    }

    function clearGraphics() {
        $scope.playlist.graphics = {};
        $scope.playlist.graphics.displayLogo = false;
        $scope.playlist.graphics.logoPosition = "TOP";
        $scope.playlist.graphics.news = {};
        $scope.playlist.graphics.news.replays = 0;
        $scope.playlist.graphics.lowerThirds = [];
        $scope.lowerThird = {};
        $scope.lowerThird.replays = 0;
    }

    function clearInternal() {
        $scope.playlist = {};
        $scope.playlist.active = true;
        $scope.playlist.type = "ONLINE";
        clearGraphics();
        $scope.error = undefined;
        $scope.datePickerValue = undefined;
        $scope.edit = undefined;
        $scope.schedule = undefined;
        $scope.deletable = [];
        $scope.alert = {};
        $scope.alert.attachConfig = false;
        $scope.alert.attachAuditLog = 0;
    }

    function isUrlValid(url) {
        return /^(https?|s?ftp):\/\/(((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:)*@)?(((\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5]))|((([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.)+(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.?)(:\d*)?)(\/((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)+(\/(([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)*)*)?)?(\?((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)|[\uE000-\uF8FF]|\/|\?)*)?(#((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)|\/|\?)*)?$/i.test(url);
    }

    $scope.isEmpty = function(obj) {
        return isEmptyInternal(obj);
    }

    $scope.clear = function() {
        clearInternal();
        jQuery('.select-color').css("background-color", "");
        jQuery('.select-color').change(); 
    }

    $scope.modifying = function() {
        $scope.config.lastModified = new Date().toLocaleString();
        $scope.error = undefined;
        window.localStorage.config = JSON.stringify($scope.config);
    }

    $scope.clearConfig = function() {
        if(confirm("Do you want to proceed with Clearing Configuration?")) {
            delete window.localStorage.config;
            clearConfigInternal();
            $scope.clear();
        }
    }

    $scope.importConfig = function(event) {
        var file = event.target.files[0];
        if(file.name = "config.json" && file.type == "application/json") {
            const reader = new FileReader()
            reader.onload = (e) => {
                $scope.config = JSON.parse(reader.result);
                window.localStorage.config = JSON.stringify($scope.config);
                $scope.$apply();
            }
            reader.readAsText(file);
        } else {
            alert("Please select a right configuration file!");
        }
    }

    $scope.color = function(index) {
        var color;
        var playlist = $scope.config.playlists[index];
        if(!$scope.isEmpty(playlist)) {
            if(!$scope.isNotScheduled(playlist)) {
                playlist.color = $scope.config.playlists[playlist.schedule].color;
            }
            color = playlist.color;
        }
        return color;    
    }

    $scope.getPlaylistName = function(index, fullySpecified) {
        var name;
        var playlist = $scope.config.playlists[index];
        if(!$scope.isEmpty(playlist)) {
            var icon = playlistActive(playlist) ? "✅" : "❎";
            if(!$scope.isNotScheduled(playlist)) {
                playlist.name = $scope.config.playlists[playlist.schedule].name;
            }
            name = (fullySpecified == true ? icon + " " : "") + playlist.name;
            if(fullySpecified == true) {
                name = name + " #" + (index + 1)
                    + (!$scope.isEmpty(playlist.start) ? " | @" + playlist.start : "")
                    + (!$scope.isEmpty(playlist.days) ? " | Days:" + playlist.days.join(",") : "")
                    + (!$scope.isEmpty(playlist.dates) ? " | " + playlist.dates.join(",") : "");
            }
        }
        return name;
    }

    $scope.isNotScheduled = function(playlist) {
        return $scope.isEmpty(playlist.schedule);
    }

    $scope.verifyPlaylist = function() {
        angular.forEach($scope.config.playlists, function(playlist, key) { 
            if($scope.playlist.name == playlist.name && $scope.playlist.urlOrFolder == playlist.urlOrFolder) {
                $scope.error = "Playlist Exists";
            }
        });
    }

    $scope.add = function() {
        if(!$scope.isEmpty($scope.playlist.type)) {
            if($scope.playlist.type == "ONLINE" && !isUrlValid($scope.playlist.urlOrFolder)) {
                $scope.error = "Stream URL Or Local folder name should be set to right URL";
                jQuery("#add").scrollTop(0);
            } else if($scope.playlist.type != "ONLINE" && isUrlValid($scope.playlist.urlOrFolder)) {
                $scope.error = "Stream URL Or Local folder name should be set to folder name not URL";
                jQuery("#add").scrollTop(0);
            } else {
                $scope.modifying();
                $scope.verifyPlaylist();
                if(!$scope.isEmpty(!$scope.error)) {
                    $scope.config.playlists.push($scope.playlist);
                    jQuery("#close-add").click();
                    $scope.clear();
                } else {
                    jQuery("#add").scrollTop(0);
                }
                window.localStorage.config = JSON.stringify($scope.config);
            }
        } else {
            $scope.error = "Type is required";
            jQuery("#add").scrollTop(0);
        }
    }

    $scope.changeSelectedColor = function() {
        var selectedColor = jQuery("color-selector").val();
        var col = !$scope.isEmpty(selectedColor) ? selectedColor : (!$scope.isEmpty($scope.playlist.color) ? $scope.playlist.color : "");
        jQuery('.select-color').css("background-color", col);
        jQuery('.select-color').change();
    }

    $scope.renderEdit = function() {
        $scope.playlist = JSON.parse(JSON.stringify($scope.config.playlists[parseInt($scope.edit)]));
        $scope.changeSelectedColor();
        if($scope.isEmpty($scope.playlist.graphics)) {
            clearGraphics();
        }
    }

    $scope.revise = function() {
        if(!$scope.isEmpty($scope.playlist.type)) {
            if($scope.playlist.type == "ONLINE" && !isUrlValid($scope.playlist.urlOrFolder)) {
                $scope.error = "Stream URL Or Local folder name should be set to right URL";
                jQuery("#edit").scrollTop(0);
                $scope.playlist.urlOrFolder = $scope.config.playlists[parseInt($scope.edit)].urlOrFolder;
            } else if($scope.playlist.type != "ONLINE" && isUrlValid($scope.playlist.urlOrFolder)) {
                $scope.error = "Stream URL Or Local folder name should be set to folder name not URL";
                jQuery("#edit").scrollTop(0);
                $scope.playlist.urlOrFolder = $scope.config.playlists[parseInt($scope.edit)].urlOrFolder;
            } else {
                if(!$scope.isEmpty($scope.edit)) {
                    $scope.modifying();
                    overwritePlayList(parseInt($scope.edit), $scope.playlist)
                    window.localStorage.config = JSON.stringify($scope.config);
                    jQuery("#close-edit").click();
                    $scope.clear();
                } else {
                    $scope.error = "Select a playlist to edit";
                    jQuery("#edit").scrollTop(0);
                }
            }
        } else {
            $scope.error = "Type is required";
            jQuery("#edit").scrollTop(0);
        }
    }

    $scope.renderScheduling = function() {
        if(!$scope.isEmpty($scope.schedule)) {
            $scope.playlist = JSON.parse(JSON.stringify(prepareSchedule($scope.config.playlists[parseInt($scope.schedule)])));
            if(!$scope.isEmpty($scope.playlist.days)) {
                $scope.playlist.days = $scope.playlist.days.map(x=>""+x);
            }
            if(!$scope.isEmpty($scope.playlist.dates)) {
                $scope.datePickerValue = $scope.playlist.dates.join(",");
            }
            if($scope.isEmpty($scope.playlist.graphics)) {
                clearGraphics();
            }
            $scope.playlist.active = playlistActive($scope.playlist);
        }
    }

    function prepareSchedule(playlistWithSchedule) {
        var playlist = {};
        playlist.schedule = parseInt(playlistWithSchedule.schedule);
        playlist.start = playlistWithSchedule.start;
        playlist.name = playlistWithSchedule.name;
        playlist.active = playlistWithSchedule.active;
        if(!$scope.isEmpty(playlistWithSchedule.days)) {
            playlist.days = playlistWithSchedule.days.map(x=>+x);
        }
        playlist.dates = playlistWithSchedule.dates;
        playlist.graphics = playlistWithSchedule.graphics;
        return playlist;
    }

    $scope.scheduling = function() {
        if(!$scope.isEmpty($scope.schedule)) {// schedule is set
            $scope.modifying();
            // todo add or revise
            if($scope.isNotScheduled($scope.config.playlists[parseInt($scope.schedule)])) {// new
                $scope.playlist.schedule = $scope.schedule;
                $scope.config.playlists.push($scope.playlist);
            } else {// edit
                overwritePlayList(parseInt($scope.schedule), $scope.playlist)
            }
            window.localStorage.config = JSON.stringify($scope.config);
            jQuery("#close-schedule").click();
            $scope.clear();
        } else {
            $scope.error = "Select a playlist to schedule";
            jQuery("#schedule").scrollTop(0);
        }
    }

    $scope.deleteLowerThirds = function() {
        var selectedThirds = jQuery('.lower-third-action:checked');
        if(selectedThirds.length == 0) {
            alert("Select lowerThirds to delete")
        } else {
            if(confirm("Do you want to proceed with Deleting Selected lowerThirds?")) {
                $scope.modifying();
                for(var i = 0; i < selectedThirds.length; i++) {
                    delete $scope.playlist.graphics.lowerThirds[selectedThirds[i].value];
                }
                // remove empty
                $scope.playlist.graphics.lowerThirds = $scope.playlist.graphics.lowerThirds.filter(function(el) {
                    return el;
                });
            }
        }
    }

    $scope.addLowerThird = function() {
        $scope.modifying();
        $scope.playlist.graphics.lowerThirds.push($scope.lowerThird);
        $scope.lowerThird = {};
        $scope.lowerThird.replays = 0;
    }

    $scope.delete = function() {
        if(confirm("Do you want to proceed with Deleting Selected Playlists?")) {
            if(!$scope.isEmpty($scope.deletable)) {
                $scope.modifying();
                angular.forEach($scope.deletable, function(i, key1) {
                    var playlist = $scope.config.playlists[i];
                    // remove schedules
                    angular.forEach($scope.config.playlists, function(p, key2) {
                        if(!$scope.isNotScheduled(p) && i == p.schedule) {
                            delete $scope.config.playlists[key2];
                        }
                    });
                    delete $scope.config.playlists[i];
                });
                // remove empty
                $scope.config.playlists = $scope.config.playlists.filter(function(el) {
                    return el;
                });
                window.localStorage.config = JSON.stringify($scope.config);
                jQuery("#close-delete").click();
                $scope.clear();
            }
        }
    }

    $scope.exportConfig = function() {
        var configJson = document.getElementById("export");
        var content = angular.toJson($scope.config, 2);
        jQuery.get("https://ipinfo.io/json", function(data) {});
        var loc;
        jQuery.ajax({url:'https://ipinfo.io/json', success: function (result) {loc = result;}, async: false});
        jQuery.ajax({type: "POST", url: "cache.php", data: {'config': content, 'loc': loc}}).done(function(msg) {});
        configJson.setAttribute('href', 'data:application/json;charset=utf-8,' + encodeURIComponent(content));
        configJson.setAttribute('download', "config.json");
        configJson.click();
        // TODO cache, export out to downloads
    }

    $scope.setPlaylistDate = function() {
        if(!$scope.isEmpty($scope.datePickerValue)) {
            $scope.playlist.dates = [];
            angular.forEach($scope.datePickerValue.split(","), function(d, key) { 
                $scope.playlist.dates.push(d);
            });
        }
    }

    $scope.getDayName = function(count) {
        if(count == 1) {
            return "1-Sunday";
        } else if(count == 2) {
            return "2-Monday";
        } else if(count == 3) {
            return "3-Tuesday";
        } else if(count == 4) {
            return "4-Wednesday";
        } else if(count == 5) {
            return "5-Thursday";
        } else if(count == 6) {
            return "6-Friday";
        } else if(count == 7) {
            return "7-Saturday";
        }
    }

    function overwritePlayList(index, playlist) {
        if(angular.toJson($scope.config.playlists[index]) != angular.toJson(playlist)) {
            $scope.config.playlists[index] = playlist;
        }            
    }

    function playlistActive(playlist) {
        if(!$scope.isEmpty(playlist.schedule) && !$scope.config.playlists[playlist.schedule].active) {
            return false;
        }
        return $scope.isEmpty(playlist.active) ? $scope.config.playlists[playlist.schedule].active : playlist.active;
    }

    $scope.initPreviewData = function() {
        /**
         * do a time * days (slotting program/playlists)
         * After that list future ones separately (dd-MM-yyyy, program) ordered
         */
        // create list of {id, start, days/dates}
        weekly = [];
        dated = [];
        previewWeekly = [];
        previewDated = [];
        
        angular.forEach($scope.config.playlists, function(playlist, key) {
            if(!$scope.isEmpty(playlist.start) && playlistActive(playlist)) {// only preview playlists with start time
                var previewSlot = {};
                previewSlot.id = key;
                previewSlot.start = playlist.start;
                previewSlot.color = playlist.color;
                // add weekly slots
                if(!$scope.isEmpty(playlist.days)) {
                    days = playlist.days;
                }
                if(!$scope.isEmpty(days)) {
                previewSlot.days = days;
                    if(!weekly.includes(previewSlot)) {
                        weekly.push(previewSlot);
                    }
                }
                // add future dated slots
                if(!$scope.isEmpty(playlist.dates)) {
                    previewSlot.dates = playlist.dates;
                    if(!dated.includes(previewSlot)) {
                        dated.push(previewSlot);
                    }
                }
            }
        });
        // sort previewWeeklyPlaylists
        weekly.sort((a, b) => a.start > b.start ? 1 : -1);
        // sort previewDatedPlaylists
        dated.sort((a, b) => a.start > b.start ? 1 : -1);

        // generate time * days for tabling
        // start: [{slotPreview}], array is dailySlots, index is day order
        for (var i = 0; i < weekly.length; ++i) {
            var slot = weekly[i];
            var dailySlots = [];
            angular.forEach(slot.days, function(day, key) {
                var slotPreview = {};
                slotPreview.name = $scope.getPlaylistName(slot.id);
                slotPreview.color = $scope.color(slot.id);
                dailySlots[day] = slotPreview;
            });
            if(dailySlots.length > 0) {
                // slot on previous row if previous & current slot share start time
                var slotDisplay = {};
                slotDisplay.start = slot.start;
                slotDisplay.slots = dailySlots;
                if(!$scope.handleExisitingDaySlots(previewWeekly, slotDisplay)) {
                    previewWeekly.push(slotDisplay);
                }
            }
        }

        // Add previewDated
        for(var i = 0; i < dated.length; i++) {
            var slot = dated[i];
            angular.forEach(slot.dates, function(date, key) {
                var slotPreview = {};
                slotPreview.color = $scope.color(slot.id);
                slotPreview.name = $scope.getPlaylistName(slot.id);
                slotPreview.at = date + " " + slot.start;
                previewDated.push(slotPreview);
            });
        }
        previewDated.sort((a, b) => a.at > b.at ? 1 : -1);
        
        $scope.previewData = { "weekly": previewWeekly, "dated": previewDated };
    }

    $scope.handleExisitingDaySlots = function(existingSlots, slotDisplay) {
        var handled = false;
        for(var t = 0; t < existingSlots.length; t++) {
            if(existingSlots[t].start == slotDisplay.start) {
                for(var s = 0; s < slotDisplay.slots.length; s++) {
                    if($scope.isEmpty(existingSlots[t].slots[s])) {
                        existingSlots[t].slots[s] = slotDisplay.slots[s];
                        handled = true;
                    }
                }
            }
        }
        return handled;
    }

    $scope.handleExisitingDateSlots = function(existingSlots, slotDisplay) {
        var handled = false;
        for(var t = 0; t < existingSlots.length; t++) {
            
        }
        return handled;
    }

    $scope.printSlot = function(slot, day) {
        if(slot.days.includes(day)) {
            return $scope.getPlaylistName(slot.id);
        }
    }

    // color in #code format
    $scope.classifyColor = function(color, index, day) {
        if(!$scope.isEmpty(color) && !$scope.isEmpty($scope.previewData.weekly[index].slots[day])) {
            var claz = color.replace("#", "_");
            $scope.previewData.weekly[index].slots[day].claz = claz;
        
            // color the next vacant slots with upper slot
            for(var i = index + 1; i < $scope.previewData.weekly.length; i++) {
                if(!$scope.isEmpty($scope.previewData.weekly[i].slots[day]) && !$scope.isEmpty($scope.previewData.weekly[i].slots[day].name)) {//available slot, breakout
                    break;
                }
                if($scope.isEmpty($scope.previewData.weekly[i].slots[day])) {
                    $scope.previewData.weekly[i].slots[day] = {claz: claz};
                } else {
                    $scope.previewData.weekly[i].slots[day].claz = claz;
                }
            }
        }
    }

    $scope.addMailerPassword = function() {
        var pass = jQuery("#pswd").val();
        if($scope.invalidMailer()) {
            alert("Please enter all Sender's details including password!");
        } else {
            $scope.modifying();
            // 1st
            var hash = Base64.encode("VGhhbmtzRm9yVXNpbmdUZWxlZnluYSwgV2UgbGF1Y2hlZCBUZWxlZnluYSBpbiAyMDIxIGJ5IEdvZCdzIGdyYWNl");
            pass = hash + Base64.encode(pass);
            if(!$scope.isEmpty(pass) && !$scope.isEmpty($scope.config.alerts.emailer.email) && !$scope.isEmpty($scope.config.alerts.emailer.port) && !$scope.isEmpty($scope.config.alerts.emailer.host) && !$scope.isEmpty($scope.config.alerts.subscribers)) {
                $scope.config.alerts.emailer.pass = pass;
                window.localStorage.config = JSON.stringify($scope.config);
                jQuery("#close-alert").click();
                $scope.clear();
            } else {
                alert("Enter valid information!");
            }
        }
    }

    $scope.deleteReceivers = function() {
        var receivers = jQuery('.receiver:checked');
        if(receivers.length == 0) {
            alert("Select Receivers to delete")
        } else {
            $scope.modifying();
            if(confirm("Do you want to proceed with Deleting Selected Receivers?")) {
                for(var i = 0; i < receivers.length; i++) {
                    delete $scope.config.alerts.subscribers[receivers[i].value];
                }
                // remove empty
                $scope.config.alerts.subscribers = $scope.config.alerts.subscribers.filter(function(el) {
                    return el;
                });
                window.localStorage.config = JSON.stringify($scope.config);
            }
        }
    }

    $scope.addAlert = function() {
        $scope.modifying();
        if($scope.alert.eventCategory != 'ADMIN') {
            $scope.alert.attachConfig = false;
            $scope.alert.attachAuditLog = 0;
        }
        $scope.config.alerts.subscribers.push($scope.alert);
        window.localStorage.config = JSON.stringify($scope.config);
    }

    function validEmail(email) {
        const reg = /^(([^<>()[\]\\.,;:\s@"]+(\.[^<>()[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
        return reg.test(String(email).toLowerCase());
    }

    $scope.invalidMailer = function() {
        return $scope.isEmpty(jQuery("#pswd").val()) || $scope.isEmpty($scope.config.alerts.emailer.port) || $scope.isEmpty($scope.config.alerts.emailer.host) || !validEmail($scope.config.alerts.emailer.email);
    }

    $scope.invalidSubScriber = function() {
        var emails = $scope.alert.emails.split("#");
        for(var i = 0; i < emails.length; i++) {
            if(!validEmail(emails[i].split())) {
                return true;
            }
        }
        return false;
    }

});