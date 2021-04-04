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
public class LowerThird {
    // file name/path within lowerThird directory
    private String file;
    // minutes to start separated by #
    private String starts = "0";
    // number of times to play
    private int replays = 0;

    public Double[] getStartsArray() {
        List<Double> startTimes = new ArrayList<>();
        if(StringUtils.isNotBlank(starts)) {
            Arrays.stream(starts.split(Graphics.MESSAGE_SPLITTER)).forEach(start -> {
                if(StringUtils.isNotBlank(start)) {
                    startTimes.add(Double.parseDouble(start.trim()));
                }
            });
            Collections.sort(startTimes);
        }
        return startTimes.toArray(new Double[startTimes.size()]);
    }
}
