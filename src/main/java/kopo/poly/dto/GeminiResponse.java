package kopo.poly.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiResponse {
    private List<Candidate> candidates;

    public List<Candidate> getCandidates() { return candidates; }
    public void setCandidates(List<Candidate> candidates) { this.candidates = candidates; }

    public static class Candidate {
        private Content content; // 객체로 변경

        public Content getContent() { return content; }
        public void setContent(Content content) { this.content = content; }
    }

    public static class Content {
        private List<Part> parts;

        public List<Part> getParts() { return parts; }
        public void setParts(List<Part> parts) { this.parts = parts; }
    }

    public static class Part {
        private String text;

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }
}