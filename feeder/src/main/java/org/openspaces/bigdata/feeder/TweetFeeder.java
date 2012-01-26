/*
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openspaces.bigdata.feeder;

import java.util.Formatter;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.social.twitter.api.Tweet;
import org.springframework.social.twitter.api.Twitter;
import org.springframework.social.twitter.api.impl.TwitterTemplate;
import org.springframework.stereotype.Component;

import com.gigaspaces.document.DocumentProperties;
import com.gigaspaces.document.SpaceDocument;

/**
 * A feeder bean that connects to Twitter, retrieves the public timeline tweets, converts them to standard Tweet SpaceDocument format, and writes them to the
 * remote space.
 * 
 * @author Dotan Horovits
 */
@Component
public class TweetFeeder {
    private Logger log = Logger.getLogger(this.getClass().getName());
    @Autowired
    private GigaSpace gigaSpace;
    @Value("${tweet.delayInMs:1000}")
    private int delayInMs = 1000; 
    @Value("${tweet.periodInMs:1000}")
    private int periodInMs = 1000; 

    public static void main(String[] args) {
        new TweetFeeder().execute();
    }

    @PostConstruct
    public void execute() {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            Formatter formatter = new Formatter();

            @Override
            public void run() {

                try {
                    Twitter twitter = new TwitterTemplate();

                    List<Tweet> tweets = twitter.timelineOperations().getPublicTimeline();

                    for (Tweet tweet : tweets) {
                        log.fine(formatter.format("Tweet id=%d\tfromUser=%s\ttext=%s \n", tweet.getId(), tweet.getFromUser(), tweet.getText()).toString());
                        // System.out.format("Tweet id=%d\tfromUser=%s\ttext=%s \n",
                        // tweet.getId(), tweet.getFromUser(), tweet.getText());
                        gigaSpace.write(constructTweetDocument(tweet));
                    }
                } catch (DataAccessException e) {
                    log.severe("error feeding tweets: " + e.getMessage());
                }
            }
        }, delayInMs, periodInMs);
    }

    public SpaceDocument constructTweetDocument(Tweet tweet) {
        DocumentProperties properties = new DocumentProperties().setProperty("Id", tweet.getId()).setProperty("Text", tweet.getText())
                .setProperty("CreatedAt", tweet.getCreatedAt()).setProperty("FromUserId", tweet.getFromUserId()).setProperty("ToUserId", tweet.getToUserId())
                .setProperty("Processed", Boolean.FALSE);
        SpaceDocument tweetDoc = new SpaceDocument("Tweet", properties);
        return tweetDoc;
    }

}
