package com.example.kyon;

import android.graphics.RectF;

public interface Detection {

    public class Recognition {

        private final String id;
        private final String title;
        private final Float confidence;
        private RectF location;

        public Recognition(
                final String id, final String title, final Float confidence, final RectF location) {

            this.id = id;
            this.title = title;
            this.confidence = confidence;
            this.location = location;
        }

        public String getTitle() {
            return title;
        }

        public Float getConfidence() {
            return confidence;
        }

        public RectF getLocation() {
            return new RectF(location);
        }
    }

}
