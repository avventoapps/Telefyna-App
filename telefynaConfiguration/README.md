c# Telefyna Configuration

This project contains a client side application for managing Telefyna's configurations which includes programming/scheduling

## TODO
- [ ] fix fillers, music is played first, 
- [ ] first resuming one is skipped
- [ ] fix audio delay: https://stackoverflow.com/questions/63754900/exoplayer-mediaplayer-android-delay-applying-playbackparameters
    - reinitiate player on every play/switch
    - parhaps that ensulo video drops frames etc which affects encoding/decoding
- [ ] changing playlist active/graphics should instantly change all scheduled ones. add a function to do this and send event to it
- [ ] fix ERROR: java.lang.IllegalArgumentException: Unexpected runtime error immediately after switching 
    - added a quick way out, ensure it causes no delays whatsover
- preview, ensure last schedule is showed
- remove bumpers section from all resuming
- seems sequency transitioning is ordered in reverse, investigate and fix
- CREATE playlist should position it immediately before schedules in positioning bot just as last item


- [ ] Bug: one message in ticket hangs on the screen, tickerView: fix with one message
- [ ] support bumpers for all local programs
- [ ] fix readme on web config
- [ ] not syncing in the set 5 minutes
- [ ] fix error after switch
- [ ] move fillers to sd/telefynaFiller and have playlist on internal as audit, merge audit or telefyna
- [ ] fix relaunching app on icon press on tv
- [ ] Add delay to playlist
- [ ] add resuming_one category of playlist which plays one program only and resumes_next
- [ ] 3ABN: install programs, fillers
- [ ] 3ABN: work on drive sync with broadcast imac and telefyna, test sync times etc and init.txt
- [ ] 3ABN: finish schedule dialogs with uganda & international
- [ ] Replace less secure apps with request access for telefyna app
- [ ] fix buttons hidden on mobile view
- [ ] outline current slot and keep the tracker progressing
- [ ] support php receivers on exporting config
- [ ] create a qa testing framework to ensure the ui generator works well
- [ ] add current/previous/next program tracking on the schedule preview | Not necessary
- [x] suppourt channel colors at: https://r.3abn.org/sched/latest/
- [x] move playlist selection into a separate php and on selection display below it a discription
- [x] add delete all schedules button
- [x] remove number from the name of the day of the week
- [x] add system metrics export (part of maintanance&configExport) to email alers for php & java; (cpu, disk, users etc)
- [x] support mobile, fix table etc scalling and popups plus cache refresh
- [x] put active check on scheduling for overriding, ensure inactive is out of preview
- [x] add space after ________
- [x] add telefynaBot on footer of export email, also 'This is a system message and no need to respond to it' to be there
- [x] UI info should contain and protection phrase for config etc
- [x] Increase numbers of encoding and decoding real password to increase security, rename encrypting lib and methods
- [x] write a way to share system errors with admin and attach auditLog and periodical logs
- [x] test out videoView/lowerThirds again with the view last on graphics.xml
- [x] graphics audit
- [x] schedule to contain graphics, if replays isn't set. the ticker will show all through
- [x] handle defaults to match with android
- [x] support graphics configuration
- [x] fix or support caching config on client side
- [x] fill up colors for proceeding slots
- [x] Build entire schedule preview
- [x] Binding config.json to dom elements in memory
- [x] Import & export config.json
- [x] Automation, Notification Disabling triggering etc
- [x] Adding playlists
- [x] Scheduling & Editing playlists
- [x] Listing ordered playlists and rendering clones right
- [x] Delete playlist(s)
- [x] Realtime change config.json updating and validation