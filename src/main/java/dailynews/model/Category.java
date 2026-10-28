package dailynews.model;

/**
 * 뉴스레터 카테고리. 선언 순서가 곧 화면·문서의 표시 순서다.
 */
public enum Category {
    POLITICS("정치", "🏛", "politics"),
    SOCIETY("사회", "👥", "society"),
    ECONOMY("경제", "💰", "economy"),
    IT("IT", "💻", "it"),
    GLOBAL("글로벌", "🌍", "global");

    private final String label;
    private final String emoji;
    private final String slug;

    Category(String label, String emoji, String slug) {
        this.label = label;
        this.emoji = emoji;
        this.slug = slug;
    }

    public String label() {
        return label;
    }

    public String emoji() {
        return emoji;
    }

    /** HTML 섹션 id·앵커에 쓰는 영문 소문자 이름 */
    public String slug() {
        return slug;
    }
}
