jQuery(function() {
    jQuery('.date').datepicker({
        multidate: true,
        format: 'dd-mm-yyyy',
        todayHighlight: true,
        startDate: new Date()
    });
    jQuery('.timepicker').timepicker({'timeFormat': 'H:i'});
    jQuery('.color-selector').colorselector();
});

angular.module("Telefyna", ['ngCookies']).controller('Config', function($cookies, $scope) {
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
        $scope.config.alerts.enabled = true;
        $scope.config.alerts = {};
        $scope.config.alerts.mailer = {};
        $scope.config.alerts.mailer.host = "smtp.gmail.com";
        $scope.config.alerts.mailer.port = 587;
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
        $scope.playlist.graphics.news.speed = "SLOW";
        $scope.playlist.graphics.lowerThirds = [];
        $scope.lowerThird = {};
        $scope.lowerThird.replays = 0;
    }

    function clearInternal() {
        $scope.overrideSchedules = false;
        $scope.playlist = {};
        $scope.playlist.active = true;
        $scope.playlist.type = "ONLINE";
        $scope.playlist.usingExternalStorage = false;
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
        jQuery("#pswd").val("");
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
        let file = event.target.files[0];
        if(file.name = "config.json" && file.type == "application/json") {
            const reader = new FileReader();
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
        let color;
        let playlist = $scope.config.playlists[index];
        if(!$scope.isEmpty(playlist)) {
            if(!$scope.isNotScheduled(playlist)) {
                playlist.color = $scope.config.playlists[playlist.schedule].color;
            }
            color = playlist.color;
        }
        return color;    
    }

    $scope.getPlaylistName = function(index, fullySpecified) {
        let name;
        let playlist = $scope.config.playlists[index];
        if(!$scope.isEmpty(playlist)) {
            let icon = playlistActive(playlist) ? "✅" : "❎";
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
        let selectedColor = jQuery("color-selector").val();
        let col = !$scope.isEmpty(selectedColor) ? selectedColor : (!$scope.isEmpty($scope.playlist.color) ? $scope.playlist.color : "");
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
                    overwritePlayList(parseInt($scope.edit), $scope.playlist);
                    if(!$scope.isEmpty($scope.overrideSchedules)) {
                        overwriteSchedules(parseInt($scope.edit), $scope.playlist);
                    }
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
        let playlist = {};
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
                overwritePlayList(parseInt($scope.schedule), $scope.playlist);
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
        let selectedThirds = jQuery('.lower-third-action:checked');
        if(selectedThirds.length == 0) {
            alert("Select lowerThirds to delete");
        } else {
            if(confirm("Do you want to proceed with Deleting Selected lowerThirds?")) {
                $scope.modifying();
                for(let i = 0; i < selectedThirds.length; i++) {
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
                    let playlist = $scope.config.playlists[i];
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
        let configJson = document.getElementById("export");
        $scope.config.playlists.sort(function(a, b) {
            if (a.start > b.start) {
                return 1;
            } if (a.start < b.start) {
                return -1;
            }
        });
        let content = angular.toJson($scope.config, 2);
        jQuery.get("https://ipinfo.io/json", function(data) {});
        let loc;
        jQuery.ajax({url:'https://ipinfo.io/json', success: function (result) {loc = result;}, async: false});
        jQuery.ajax({type: "POST", url: "cache.php", data: {'config': $scope.config, 'loc': loc}}).done(function(msg) {});
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
            return "Sunday";
        } else if(count == 2) {
            return "Monday";
        } else if(count == 3) {
            return "Tuesday";
        } else if(count == 4) {
            return "Wednesday";
        } else if(count == 5) {
            return "Thursday";
        } else if(count == 6) {
            return "Friday";
        } else if(count == 7) {
            return "Saturday";
        }
    }

    function overwriteSchedules(index, playlist) {
        angular.forEach($scope.config.playlists, function(p, k) {
            if(!$scope.isNotScheduled(p) && index == p.schedule) {
                $scope.config.playlists[k].active = playlist.active;
                $scope.config.playlists[k].graphics = playlist.graphics;
            }
        });
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
                let previewSlot = {};
                previewSlot.id = key;
                previewSlot.start = playlist.start;
                previewSlot.color = playlist.color;
                // add weekly slots
                let allDays = [1, 2, 3, 4, 5, 6, 7];
                if(!$scope.isEmpty(playlist.days)) {
                    allDays = playlist.days;
                }
                if(!$scope.isEmpty(allDays)) {
                    previewSlot.days = allDays;
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
        for (let i = 0; i < weekly.length; ++i) {
            let slot = weekly[i];
            let dailySlots = [];
            angular.forEach(slot.days, function(day, key) {
                let slotPreview = {};
                slotPreview.name = $scope.getPlaylistName(slot.id);
                slotPreview.color = $scope.color(slot.id);
                dailySlots[day] = slotPreview;
            });
            if(dailySlots.length > 0) {
                // slot on previous row if previous & current slot share start time
                let slotDisplay = {};
                slotDisplay.start = slot.start;
                slotDisplay.slots = dailySlots;
                if(!$scope.handleExisitingDaySlots(previewWeekly, slotDisplay)) {
                    previewWeekly.push(slotDisplay);
                }
            }
        }

        // Add previewDated
        for(let i = 0; i < dated.length; i++) {
            let slot = dated[i];
            angular.forEach(slot.dates, function(date, key) {
                let slotPreview = {};
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
        let handled = false;
        for(let t = 0; t < existingSlots.length; t++) {
            if(existingSlots[t].start == slotDisplay.start) {
                for(let s = 0; s < slotDisplay.slots.length; s++) {
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
        let handled = false;
        for(let t = 0; t < existingSlots.length; t++) {
            
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
            let claz = color.replace("#", "_");
            $scope.previewData.weekly[index].slots[day].claz = claz;
        
            // color the next vacant slots with upper slot
            for(let i = index + 1; i < $scope.previewData.weekly.length; i++) {
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
        let pass = jQuery("#pswd").val();
        if($scope.invalidMailer()) {
            alert("Please enter all Sender's details including password!");
        } else {
            $scope.modifying();
            // 1st: 2nd
            let hash = B.encode(B.encode("VGhhbmtzRm9yVXNpbmdUZWxlZnluYSwgV2UgbGF1Y2hlZCBUZWxlZnluYSBpbiAyMDIxIGJ5IEdvZCdzIGdyYWNl"));// 5
            pass = B.encode(B.encode(B.encode(B.encode(B.encode(pass))))) + hash + Math.floor((Math.random() * 9) + 1);
            if(!$scope.isEmpty(pass) && !$scope.isEmpty($scope.config.alerts.mailer.email) && !$scope.isEmpty($scope.config.alerts.mailer.port) && !$scope.isEmpty($scope.config.alerts.mailer.host) && !$scope.isEmpty($scope.config.alerts.subscribers)) {
                $scope.config.alerts.mailer.pass = pass;
                window.localStorage.config = JSON.stringify($scope.config);
                jQuery("#close-alert").click();
                $scope.clear();
            } else {
                alert("Enter valid information!");
            }
        }
    }

    $scope.deleteReceivers = function() {
        let receivers = jQuery('.receiver:checked');
        if(receivers.length == 0) {
            alert("Select Receivers to delete");
        } else {
            $scope.modifying();
            if(confirm("Do you want to proceed with Deleting Selected Receivers?")) {
                for(let i = 0; i < receivers.length; i++) {
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
        return $scope.isEmpty(jQuery("#pswd").val()) || $scope.isEmpty($scope.config.alerts.mailer.port) || $scope.isEmpty($scope.config.alerts.mailer.host) || !validEmail($scope.config.alerts.mailer.email);
    }

    $scope.invalidSubScriber = function() {
        let emails = $scope.alert.emails.split("#");
        for(let i = 0; i < emails.length; i++) {
            if(!validEmail(emails[i].split())) {
                return true;
            }
        }
        return false;
    }

    $scope.getPlaylistTypeDesc = function(category) {
        if(category == "ONLINE") {
            return "An Online streaming playlist using a stream url with NO support for bumper";
        } else if(category == "LOCAL_SEQUENCED") {
            return "A local playlist starting from the first to the last alphabetical program by file naming with support for bumpers";
        } else if(category == "LOCAL_RANDOMIZED") {
            return "A local playlist randlomy selecting programs with support for bumpers";
        } else if(category == "LOCAL_RESUMING") {
            return "A local playlist resuming from the previous program at exact stopped time with NO support for bumper";
        } else if(category == "LOCAL_RESUMING_ONE") {
            return "A local one program selection playlist resuming from the next program with NO support for bumper";
        } else if(category == "LOCAL_RESUMING_SAME") {
            return "A local playlist restarting the previous non completed program on the next playout with NO support for bumper";
        } else if(category == "LOCAL_RESUMING_NEXT") {
            return "A local playlist resuming from the next program with NO support for bumper";
        }
    }

    $scope.deleteAllSchedules = function() {
        if(confirm("Do you want to proceed with Deleting all existing schedules?")) {
            let indices = [];
            angular.forEach($scope.config.playlists, function(playlist, key) { 
                if(!$scope.isNotScheduled(playlist)) {
                    delete $scope.config.playlists[key];
                    indices.push(key);
                }
            });
            if($scope.isEmpty(indices)) {
                alert("There are no schedules to delete!");
            } else {
                // remove empty
                $scope.config.playlists = $scope.config.playlists.filter(function(el) {
                    return el;
                });
            }
        }
    }

});