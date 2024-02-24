package io.ballerina.scan.utilities;

import picocli.CommandLine;

import java.util.Arrays;
import java.util.List;

public class StringToListConverter implements CommandLine.ITypeConverter<List<String>> {

    @Override
    public List<String> convert(String value) {
        // Remove white spaces between commas and split into a list
        return Arrays.asList(value.split("\\s*,\\s*"));
    }
}
