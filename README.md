![Telefyna](https://avventohome.org/wp-content/uploads/2020/12/telefyna.png "Telefyna")

# Telefyna
An android online/local streaming and scheduling player for audio and video suitable for TVs and radios.
______

## Installation
* Download the [APK from here](https://github.com/avventoapps/avvento/releases/latest/download/telefyna.apk) and install it, grant the app Storage permission and reload it if necessary

## Configurations
* Use `config.json` to create your own configurations, [here is a sample](https://github.com/avventoapps/Telefyna/blob/master/config.json), add it to either `sdcard` if existing /device drive in a folder called `telefyna`
* Add a local playlist folder containing your local programs folder for each local playlist
* Ensure the telefyna app is granted storage permission in your permissions

### Device
* set `disableNotifications` to false to disable notifications

### FTP Remote access
* If you want to access the filesystem remove, run an [FTP app like swiftp](https://f-droid.org/packages/be.ppareit.swiftp_free)
* You can use FTP clients like [FileZilla](https://filezilla-project.org/) to upload both revised `config.json` and `playlist` folder/contents

### Playlist
* Ensure the device's Date and Timezone are set correctly
* The first playlist is the default  playlist and is used as a filler if nothing is available to play next or local folder is vacant
* The second playlist is a second default, it's a second filler choice and is played when `ONLINE` ones fail because of of internet issues
* If an internet connection is lost during a broadcast, the second playlist is defaulted to and if it's restored, the previous program will be restored too
* Both the above two default playlists must be maintained active, if any of them is local, better set resuming
* If you intend to use one playlist as default for both the first and second, make the second a clone of the first
* `name` your playlist meaningfully
* `description` contains your explanation of about the playlist
* `days` of the week (`1-7`=Sun-Sat): if null or not defined, runs daily
* `dates` to schedule playlist for, date format must be `dd-MM-yyyy`
* Playlist `start` should be in format `HH:mm` eg `12:00` for mid-day, hours are in 24 hour
* `urlOrFolder`, stream url or local folder containing alphabetically ordered playlist folders
* For local playlists, if active and nothing is defined or completed, the default playlist will be played
* `type` can either be `ONLINE` (stream/default), or `LOCAL_SEQUENCED` (local ordered folder) or `LOCAL_RESUMING` (local resuming folder), or `LOCAL_RESUMING_NEXT` (local resuming from next program) or `LOCAL_RANDOMIZED` (local random folder)
* For `type`s `LOCAL_SEQUENCED`, `LOCAL_RANDOMIZED` and `LOCAL_RESUMING_NEXT`, you can define bumpers to play as the playlist starts in `bumper` folder in a sub folder named by playlist folder
* All playlist are enabled by default, to disable one, set `active=false`
* `clone` allows you to copy a playlist defined up by its order/number/count (starts from 0) and manage separate/override `day`, `repeats`, `start`
* A field left out in config is by default set to `null`
* Ensure to have a playlist completing/ending each local one else the default one will automatically be played

## Support
For any questions or queries, please email the support team at apps@avventohome.org


## TODO
- [ ] test and fix mid night runner
- [ ] Schedule once per start, ignore the rest of the slots
- [ ] add promos/sweepers/something folder that starts the playout whether in continuing. usable for upnexts: intros folder containing another named by foldername: test symbolic links
- [ ] add fading mechanisms than cut
- [ ] look through TODOs
- [ ] locally backup/download streaming content
- [ ] Build schedule builder & viewer for the `config.json`
- [ ] write tests
- [ ] play video with different or additional audio/slave
- [ ] float another layer on video stream for ads, logo etc
- [ ] read satellite channels and decoders as we do local playlists and streams
- [ ] add a way to stream video as only audio, only streaming audio
- [ ] support streaming to hls, shoutcast & loudcast (not supported); use external streaming encoder
- [ ] ensure all wrong media files are skipped (blocked)
- [ ] work on presentation approach (blocked)
- [x] Network listener, switches to second default when internet is off, and back if slot still active
- [x] Add continuing not by seekto `LOCAL_RESUMING_NEXT`
- [x] Fork and add auto start using system prefs to FTP, send [PR](https://github.com/ppareit/swiftp/pull/163)
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
