package com.careerpick.dto;

import java.util.List;

public record JobResponse(long id, long companyId, String company, String logo, String color, String title,
                          String category, String type, String location, String deadline, List<String> tags) {}
