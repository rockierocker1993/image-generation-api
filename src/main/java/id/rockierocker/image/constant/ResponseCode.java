package id.rockierocker.image.constant;

public enum ResponseCode {
    EXTENSION_NOT_SUPPORTED(
            "RC001",
            "Extensi file tidak didukung.",
            "File extension is not supported.",
            "Error Ekstensi Tidak Didukung",
            "Ekstensi file tidak didukung."
    ),
    UKNOWN_ERROR(
            "RC999",
            "Uknown error occurred.",
            "Uknown error occurred.",
            "Error",
            "Error"
    ),
    FAILED_READ_FILE(
            "RC002",
            "Failed to read input stream.",
            "Failed to read input stream.",
            "Error",
            "Error"
    ),
    PBM_CONVERSION_FAILED(
            "RC003",
            "Failed convert to PBM.",
            "Failed convert to PBM.",
            "Error",
            "Error"
    ),
    SVG_CONVERSION_FAILED(
            "RC004",
            "Failed convert to SVG.",
            "Failed convert to SVG.",
            "Error",
            "Error"
    ),
    FAILED_CREATE_TEMP_FILE(
            "RC005",
            "Failed to create temporary file.",
            "Failed to create temporary file.",
            "Error",
            "Error"
    ),
    VECTORIZE_FAILED(
            "RC006",
            "Vectorize failed.",
            "Vectorize failed.",
            "Error",
            "Error"
    ),
    VTRACE_CONFIG_NOT_FOUND(
            "RC007",
            "VTrace Config not found.",
            "VTrace Config not found.",
            "Error",
            "Error"
    ),
    PREPROCESS_FAIELD(
            "RC008",
            "Preprocess Failed.",
            "Preprocess Failed.",
            "Error",
            "Error"
    ),
    PREPROCESS_FAIELD_TO_BUFFERED_IMAGE(
            "RC009",
            "Preprocess Failed to Buffered Image.",
            "Preprocess Failed to Buffered Image.",
            "Error",
            "Error"
    ),
    ;

    private ResponseCode(String code,  String defaultMessageId, String defaultMessageEn, String defaultTitleId, String defaultTitleEn) {
        this.code = code;
        this.messageId = defaultMessageId;
        this.messageEn = defaultMessageEn;
        this.titleId = defaultTitleId;
        this.titleEn = defaultTitleEn;
    }

    private String code;
    private boolean status;
    private String messageId;
    private String messageEn;
    private String titleId;
    private String titleEn;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getMessageEn() {
        return messageEn;
    }

    public void setMessageEn(String messageEn) {
        this.messageEn = messageEn;
    }

    public String getTitleId() {
        return titleId;
    }

    public void setTitleId(String titleId) {
        this.titleId = titleId;
    }

    public String getTitleEn() {
        return titleEn;
    }

    public void setTitleEn(String titleEn) {
        this.titleEn = titleEn;
    }
}
