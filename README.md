

# Telefyna
An online/local streaming and scheduling app for video and audio
___

## Configuration
*  Use config.json to create your own, add it to either sdcard if existing /device drive in a folder called telefyna plus your playlist folder
*  Ensure the telefyna app is granted storage permission in your permissions

## Note
* `name` your playlist meaningfully
* `description` contains your explanation of about the playlist
* `day` of the week [1-7=Sun-Sat]: if null, runs daily
* Playlist `start` should be in format hhmm eg 1200 for mid-day
* `repeats` are days, if null, repeats is daily, if [], never repeats
* `urlOrFolder`, stream url or local folder containing alphabetically ordered programs or folders
* `type` can either be `LOCAL` or `STREAM` (default)
* All playlist are enabled by default, to disable one, set `active=false`
* A field left out in config is by default set to null
* `clone` allows you to copy a playlist defined up by its order/number/count and manage separate/override `active`, `day`, `repeats`, `start`

## Support
For any questions or queries, please email the support team at apps@avventohome.org


## TODO
[ ] Fix com.google.android.exoplayer2.source.BehindLiveWindowException on hls streaming
[ ] work on presentation approach
[ ] work on now playing orm to handle resuming local playlists at next play
[x] fix playlist pending extra being null in broadcast
[x] Smooth switching/
[x] Play folder
[x] Fix scheduling
[x] hide exoplayer buttons
[x] exoplayer switching to another track smoothly
[x] Support resuming from last played in playlist folder to next
