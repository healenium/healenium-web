package com.epam.healenium;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor @Getter
public enum FieldName {
    TAG("tag"), INDEX("index"), INNER_TEXT("innerText"),
    ID("id"), CLASSES("classes"), CLASS("class"), OTHER("other");

    private String fieldName;
}
