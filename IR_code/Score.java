
class Score implements Comparable<Score>{
    
    private String hashTag;
    private double score;
    
    public Score(String hashTag, double score) {
        super();
        this.hashTag = hashTag;
        this.score = score;
    }

    @Override
    /**
     *  non-ascending order
     */
    public int compareTo(Score other) {
        return -1 * Double.compare(this.score, other.score);
    }
    
    public String getHashTag() {
        return hashTag;
    }

    public double getScore() {
        return score;
    }
    
}
