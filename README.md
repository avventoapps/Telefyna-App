![Telefyna](https://avventohome.org/wp-content/uploads/2020/12/telefyna.png "Telefyna")

# Telefyna
An android online/local streaming and scheduling app for audio and video.
______

## Configurations
* Use `config.json` to create your own configurations, [here is a sample](https://github.com/avventoapps/Telefyna/blob/master/config.json), add it to either `sdcard` if existing /device drive in a folder called `telefyna` plus your local playlist folder
* Ensure the telefyna app is granted storage permission in your permissions

### Device
* set `disableNotifications` to false to disable notifications

### FTP Remote access
* If you want to access the filesystem remove, run an [FTP app like swiftp](https://f-droid.org/packages/be.ppareit.swiftp_free)
* You can use FTP clients like [FileZilla](https://filezilla-project.org/) to upload both revised config.json and playlist folder/contents

### Playlist
* Ensure the device's Date and Timezone are set correctly
* The first playlist is the default  playlist and is used as a filler if nothing is available to play next or local folder is vacant
* The second playlist is a second default, it's a second filler choice and is played when `ONLINE` ones fail because of of internet issues
* Both the above two default playlists must be maintained active, if any of them is local, better set resuming
* If you intend to use one playlist as default for both the first and second, make the second a clone of the first
* `name` your playlist meaningfully
* `description` contains your explanation of about the playlist
* `days` of the week (1-7=Sun-Sat): if null or not defined, runs daily
* `dates` to schedule playlist for, date format must be `dd-MM-yyyy`
* Playlist `start` should be in format hhmm eg `1200` for mid-day
* `urlOrFolder`, stream url or local folder containing alphabetically ordered playlist folders
* For local playlists, if active and nothing is defined or completed, the default playlist will be played
* `type` can either be `LOCAL` or `ONLINE` (default)
* All playlist are enabled by default, to disable one, set `active=false`
* `clone` allows you to copy a playlist defined up by its order/number/count (starts from 0) and manage separate/override `day`, `repeats`, `start`
* Set `resuming` to true only if you wish the next time a playlist is loaded to restart the last uncompleted item or next item
* A field left out in config is by default set to `null`
* Ensure to have a playlist completing/ending each local one else the default one will automatically be played

## Support
For any questions or queries, please email the support team at apps@avventohome.org


## TODO
- [ ] look through TODOs
- [ ] Fork and add auto start using system prefs to FTP, send PR
- [ ] write tests
- [ ] support streaming to hls, shoutcast & loudcast
- [ ] locally backup/download streaming content
- [ ] play video with different or additional audio/slave
- [ ] float another layer on video stream for ads, logo etc
- [ ] ensure all wrong media files are skipped (blocked)
- [ ] work on presentation approach (blocked)
- [ ] read satellite channels and decoders as we do local playlists and streams
- [x] support audit logs, mail them out
- [x] work on now playing orm(system preferences) to handle resuming local playlists at next play to support daily etc periods and future dates
- [x] add dates in addition to days
- [x] remove repeats, use days for day
- [x] reload configurations at midnight
- [x] default back to first playlist if the local playlist completes before end time
- [x] Fix com.google.android.exoplayer2.source.BehindLiveWindowException on hls streaming
- [x] fix playlist pending extra being null in broadcast
- [x] Play folder
- [x] Fix scheduling
- [x] hide exoplayer buttons
- [x] exoplayer switching to another track smoothly/Smooth switching
- [x] Support resuming from last played in playlist folder to next
