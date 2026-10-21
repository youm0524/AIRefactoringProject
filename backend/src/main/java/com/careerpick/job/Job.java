package com.careerpick.job;

import java.util.List;

public record Job(long id, String company, String logo, String color, String title, String category,
                  String type, String location, String deadline, List<String> tags) {}
