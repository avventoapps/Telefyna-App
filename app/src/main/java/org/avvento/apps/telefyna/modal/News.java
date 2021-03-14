package org.avvento.apps.telefyna.modal;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class News {
    private String messages;
    // minutes to start ticker at during program play,  2#6#8 means start and 2nd, 6th and 8th second, This time includes bumpers
    private String starts;

    // number of times to loop/play messages
    private int replays = 1;

    private static String MESSAGE_SPLITTER = "#";
    
    public Long[] getStartsArray() {
        List<Long> startTimes = new ArrayList<>();
        if(StringUtils.isNotBlank(starts)) {
            Arrays.stream(starts.split(MESSAGE_SPLITTER)).forEach(start -> {
                if(StringUtils.isNotBlank(start)) {
                    startTimes.add(Long.parseLong(start.trim()));
                }
            });
            Collections.sort(startTimes);
        }
        return startTimes.toArray(new Long[startTimes.size()]);
    }

    public String[] getMessagesArray() {
        List<String> mess = new ArrayList<>();
        if(StringUtils.isNotBlank(messages)) {
            Arrays.stream(messages.split(News.MESSAGE_SPLITTER)).forEach(m -> {
                if(StringUtils.isNotBlank(m)) {
                    mess.add( m.trim());
                }
            });
        }
        return mess.toArray(new String[mess.size()]);
    }
}
