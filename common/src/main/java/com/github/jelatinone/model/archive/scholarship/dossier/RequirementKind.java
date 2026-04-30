package com.github.jelatinone.model.archive.scholarship.dossier;

import java.math.BigDecimal;

import com.github.jelatinone.model.archive.scholarship.dossier.requirement.Activity;
import com.github.jelatinone.model.archive.scholarship.dossier.requirement.Degree;
import com.github.jelatinone.model.archive.scholarship.dossier.requirement.Education;
import com.github.jelatinone.model.archive.scholarship.dossier.requirement.Location;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum RequirementKind {

	WRITTEN_FINANCIAL_STATEMENT(Boolean.class),

	WRITTEN_PERSONAL_STATEMENT(Boolean.class),

	WRITTEN_RECOMMENDATION(Boolean.class),

	WRITTEN_ESSAY(Boolean.class),

	STUDENT_AFFILIATION(String.class),

	STUDENT_FIELD_OF_STUDY(String.class),

	STUDENT_ACTIVITY(Activity.class),

	STUDENT_EDUCATION_LEVEL(Education.class),

	STUDENT_PURSUED_DEGREE(Degree.class),

	STUDENT_LOCATION(Location.class),

	STUDENT_NEED(String.class),

	STUDENT_MERIT(String.class),

	STUDENT_IMPACT(String.class),

	STUDENT_MEMBERSHIP(String.class),

	STUDENT_MINIMUM_UNWEIGHTED_GPA(BigDecimal.class),

	STUDENT_MINIMUM_WEIGHTED_GPA(BigDecimal.class),

	STUDENT_FULL_TIME(Boolean.class),

	STUDENT_PART_TIME(Boolean.class),

	OTHER(String.class);

	Class<?> requirementType;

	public boolean accepts(Object requirement) {
		return requirementType.isInstance(requirement);
	}
}
