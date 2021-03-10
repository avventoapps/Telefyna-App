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
    if(!isEmptyInternal(window.localStorage.config)) {
        $scope.config = JSON.parse(window.localStorage.config);
    } else {
        clearConfigInternal();
    }
    clearInternal();

    function isEmptyInternal(obj) {
        return obj == undefined || obj == null || obj == "undefined" || obj == "null" || obj["length"] == 0;
    }

    function clearConfigInternal() {
        $scope.config = {};
        $scope.config.playlists = [];
    }

    function clearInternal() {
        $scope.playlist = {};
        $scope.playlist.active = false;
        $scope.error = undefined;
        $scope.datePickerValue = undefined;
        $scope.edit = undefined;
        $scope.deletable = [];
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
        $scope.edit = undefined;
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
            if(!$scope.isNotClone(playlist)) {
                playlist.color = $scope.config.playlists[playlist.clone].color;
            }
            color = playlist.color;
        }
        return color;    
    }

    $scope.name = function(index, fullySpecified) {
        var name;
        var playlist = $scope.config.playlists[index];
        if(!$scope.isEmpty(playlist)) {
            if(!$scope.isNotClone(playlist)) {
                playlist.name = $scope.config.playlists[playlist.clone].name;
            }
            name = playlist.name;
            if(fullySpecified == true) {
                name = name + " #" + (index + 1) 
                    + (!$scope.isEmpty(playlist.start) ? " | @" + playlist.start : "")
                    + (!$scope.isEmpty(playlist.days) ? " | Days:" + playlist.days.join(",") : "")
                    + (!$scope.isEmpty(playlist.dates) ? " | " + playlist.dates.join(",") : "");
            }
        }
        return name;
    }

    $scope.isNotClone = function(playlist) {
        return $scope.isEmpty(playlist.clone);
    }

    $scope.verifyPlaylist = function() {
        angular.forEach($scope.config.playlists, function(playlist, key) { 
            if($scope.playlist.name == playlist.name && $scope.playlist.urlOrFolder == playlist.urlOrFolder) {
                $scope.error = "Playlist Exisits";
            }
        });
    }

    $scope.add = function() {
        if(!$scope.isEmpty($scope.playlist.type)) {
            $scope.modifying();
            $scope.verifyPlaylist();
            if(!$scope.isEmpty(!$scope.error)) {
                $scope.config.playlists.push($scope.playlist);
                window.localStorage.config = JSON.stringify($scope.config);
                jQuery("#close-add").click();
                $scope.clear();
            } else {
                jQuery("#add").scrollTop(0);
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
        $scope.playlist = $scope.config.playlists[$scope.edit];
        $scope.changeSelectedColor();
        if(!$scope.isEmpty($scope.playlist.dates)) {
            $scope.datePickerValue =$scope.playlist.dates.join(",");
        }
    }

    $scope.revise = function() {
        if(!$scope.isEmpty($scope.playlist.type)) {
            if(!$scope.isEmpty($scope.edit)) {
                $scope.modifying();
                $scope.config.playlists[$scope.edit] = $scope.playlist;
                window.localStorage.config = JSON.stringify($scope.config);
                jQuery("#close-edit").click();
                $scope.clear();
            } else {
                $scope.error = "Select a playlist to edit";
                jQuery("#edit").scrollTop(0);
            }
        } else {
            $scope.error = "Type is required";
            jQuery("#edit").scrollTop(0);
        }
    }

    $scope.clone = function() {
        if(!$scope.isNotClone(playlist)) {// clone is set
            $scope.modifying();
            $scope.config.playlists.push($scope.playlist);
            jQuery("#close-clone").click();
            $scope.clear();
        } else {
            $scope.error = "Select a playlist to copy";
            jQuery("#clone").scrollTop(0);
        }
    }

    $scope.delete = function() {
        if(confirm("Do you want to proceed with Deleting Selected Playlists?")) {
            if(!$scope.isEmpty($scope.deletable)) {
                $scope.modifying();
                angular.forEach($scope.deletable, function(i, key1) {
                    var playlist = $scope.config.playlists[i];
                    // remove clones
                    angular.forEach($scope.config.playlists, function(p, key2) {
                        if(!$scope.isNotClone(p) && i == p.clone) {
                            delete $scope.config.playlists[key2];
                        }
                    });
                    delete $scope.config.playlists[i];
                });
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
        configJson.setAttribute('href', 'data:application/json;charset=utf-8,' + encodeURIComponent(angular.toJson($scope.config, 2)));
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
            if(!$scope.isEmpty(playlist.start)) {// only preview playlists with start time
                var previewSlot = {};
                previewSlot.id = key;
                previewSlot.start = playlist.start;
                previewSlot.color = playlist.color;
                // add weekly slots
                var days = [];
                if(!$scope.isEmpty(playlist.days)) {
                    days = playlist.days;
                } else {
                    days = [1, 2, 3, 4, 5, 6, 7];
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
                slotPreview.name = $scope.name(slot.id);
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
                slotPreview.name = $scope.name(slot.id);
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
            return $scope.name(slot.id);
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
});