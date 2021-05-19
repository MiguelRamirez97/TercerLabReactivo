package com.example;


import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class HelperKata {
    private static final  String EMPTY_STRING = "";
    private static String anteriorBono = null;

    public static Flux<CouponDetailDto> getListFromBase64File(final String fileBase64) {

        try (InputStream inputStream = new ByteArrayInputStream(decodeBase64(fileBase64));
             BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        ) {
            AtomicInteger counter = new AtomicInteger(0);
            String characterSeparated = FileCSVEnum.CHARACTER_DEFAULT.getId();
            Set<String> codes = new HashSet<>();
            return Flux.fromIterable(
                    bufferedReader.lines().skip(1)
                            .map(line -> getTupleOfLine(line, line.split(characterSeparated), characterSeparated))
                            .map(tuple -> {
                                String dateValidated = null;
                                String errorMessage;
                                String bonoForObject = null;
                                String bonoEnviado;


                                errorMessage = getErrorMessage(tuple,codes);
                                if (errorMessage == null){
                                    dateValidated = tuple.getT2();
                                }

                                bonoEnviado = tuple.getT1();
                                bonoForObject = getBonoForObject(bonoForObject,bonoEnviado);

                                return CouponDetailDto.aCouponDetailDto()
                                        .withCode(bonoForObject)
                                        .withDueDate(dateValidated)
                                        .withNumberLine(counter.incrementAndGet())
                                        .withMessageError(errorMessage)
                                        .withTotalLinesFile(1)
                                        .build();
                            }).collect(Collectors.toList())

            );

        } catch (IOException e) {
            e.printStackTrace();
        }
        return Flux.empty();
    }

    private static Flux<String> createFluxFrom(String fileBase64) {
        return Flux.using(() -> new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(decodeBase64(fileBase64))
                )).lines(),
                Flux::fromStream,
                Stream::close
        );
    }

    public static String typeBono(String bonoIn) {
        String typeBono;
        if (bonoEAN13(bonoIn)) {
            typeBono = ValidateCouponEnum.EAN_13.getTypeOfEnum();
        }
        else if (bonoEAN39(bonoIn)) {
            typeBono = ValidateCouponEnum.EAN_39.getTypeOfEnum();
        }
        else {
            typeBono = ValidateCouponEnum.ALPHANUMERIC.getTypeOfEnum();
        }
        return typeBono;
    }

    public static Boolean bonoEAN13(String bonoIn){
        return bonoIn.chars().allMatch(Character::isDigit) && bonoIn.length() >= 12 && bonoIn.length() <= 13;
    }

    public static Boolean bonoEAN39(String bonoIn){
        return bonoIn.startsWith("*") && bonoIn.replace("*", "").length() >= 1
                && bonoIn.replace("*", "").length() <= 43;
    }

    public static String getErrorMessage(Tuple2<String,String> tuple,Set<String> codes){
        String errorMessage;
        if (tuple.getT1().isBlank() || tuple.getT2().isBlank()) {
            errorMessage = ExperienceErrorsEnum.FILE_ERROR_COLUMN_EMPTY.toString();
        }
        else if (!codes.add(tuple.getT1())) {
            errorMessage = ExperienceErrorsEnum.FILE_ERROR_CODE_DUPLICATE.toString();
        }
        else if (!validateDateRegex(tuple.getT2())) {
            errorMessage = ExperienceErrorsEnum.FILE_ERROR_DATE_PARSE.toString();
        }
        else if (validateDateIsMinor(tuple.getT2())) {
            errorMessage = ExperienceErrorsEnum.FILE_DATE_IS_MINOR_OR_EQUALS.toString();
        }
        else {
            errorMessage = null;
        }
        return errorMessage;
    }

    public static boolean validateDateRegex(String dateForValidate) {
        String regex = FileCSVEnum.PATTERN_DATE_DEFAULT.getId();
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(dateForValidate);
        return matcher.matches();
    }

    private static byte[] decodeBase64(final String fileBase64) {
        return Base64.getDecoder().decode(fileBase64);

    }

    private static Tuple2<String, String> getTupleOfLine(String line, String[] array, String characterSeparated) {
        return Objects.isNull(array) || array.length == 0
                ? Tuples.of(EMPTY_STRING, EMPTY_STRING)
                : array.length < 2
                ? line.startsWith(characterSeparated)
                ? Tuples.of(EMPTY_STRING, array[0])
                : Tuples.of(array[0], EMPTY_STRING)
                : Tuples.of(array[0], array[1]);
    }

    public static boolean validateDateIsMinor(String dateForValidate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(FileCSVEnum.PATTERN_SIMPLE_DATE_FORMAT.getId());
            Date dateActual = sdf.parse(sdf.format(new Date()));
            Date dateCompare = sdf.parse(dateForValidate);
            return dateCompare.compareTo(dateActual) <= 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String getBonoForObject(String bonoForObject,String bonoEnviado){
        if (anteriorBono == null || anteriorBono.equals("")) {
            anteriorBono = typeBono(bonoEnviado);
            if (anteriorBono.equals("")) {
                bonoForObject = null;
            } else {
                bonoForObject = bonoEnviado;
            }
        } else if (anteriorBono.equals(typeBono(bonoEnviado))) {
            bonoForObject = bonoEnviado;
        } else if (!anteriorBono.equals(typeBono(bonoEnviado))) {
            bonoForObject = null;
        }
        return bonoForObject;
    }
}
