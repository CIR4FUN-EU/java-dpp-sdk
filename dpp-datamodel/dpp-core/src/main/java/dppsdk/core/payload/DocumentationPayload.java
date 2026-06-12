package dppsdk.core.payload;

/**
 * Transport POJO for {@link dppsdk.core.model.Documentation}.
 */
public class DocumentationPayload {

    private String digitalInstructionsLink;
    private String safetyInstructionsLink;
    private boolean downloadable;
    private Integer availableForYears;
    private boolean paperCopyAvailableOnRequest;

    public DocumentationPayload() {}

    public String getDigitalInstructionsLink() { return digitalInstructionsLink; }
    public void setDigitalInstructionsLink(String digitalInstructionsLink) { this.digitalInstructionsLink = digitalInstructionsLink; }

    public String getSafetyInstructionsLink() { return safetyInstructionsLink; }
    public void setSafetyInstructionsLink(String safetyInstructionsLink) { this.safetyInstructionsLink = safetyInstructionsLink; }

    public boolean isDownloadable() { return downloadable; }
    public void setDownloadable(boolean downloadable) { this.downloadable = downloadable; }

    public Integer getAvailableForYears() { return availableForYears; }
    public void setAvailableForYears(Integer availableForYears) { this.availableForYears = availableForYears; }

    public boolean isPaperCopyAvailableOnRequest() { return paperCopyAvailableOnRequest; }
    public void setPaperCopyAvailableOnRequest(boolean paperCopyAvailableOnRequest) { this.paperCopyAvailableOnRequest = paperCopyAvailableOnRequest; }
}


