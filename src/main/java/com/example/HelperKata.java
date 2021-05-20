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
import java.util.stream.Stream;


public class HelperKata {
    private static final  String EMPTY_STRING = "";
    private static String anteriorBono = null;

    public static Flux<CouponDetailDto> getListFromBase64File(final String fileBase64) {
        AtomicInteger counter = new AtomicInteger(0);
        String characterSeparated = FileCSVEnum.CHARACTER_DEFAULT.getId();
        Set<String> codes = new HashSet<>();
        return createFluxFrom(fileBase64).skip(1)
                .map(line -> getTupleOfLine(line, line.split(characterSeparated), characterSeparated))
                .map(tuple -> getCouponDetailDto(counter, codes, tuple));
    }

    private static Flux<String> createFluxFrom(String fileBase64) {
        return Flux.using(() -> new BufferedReader(new InputStreamReader(
                        new ByteArrayInputStream(decodeBase64(fileBase64))
                )).lines(),
                Flux::fromStream,
                Stream::close
        );
    }

    private static Tuple2<String, String> getTupleOfLine(String line, String[] array, String characterSeparated) {
        Tuple2<String, String> tuple = Tuples.of(array[0], array[1]);
        if(nullOrEmpty(array)){
            tuple = Tuples.of(EMPTY_STRING, EMPTY_STRING);
        }else if (array.length < 2){
            tuple = line.startsWith(characterSeparated) ?
                    Tuples.of(EMPTY_STRING, array[0]) : Tuples.of(array[0], EMPTY_STRING);
        }
        return tuple;
    }

    public static boolean nullOrEmpty(String[] array){
        return Objects.isNull(array) || array.length == 0;
    }

    private static CouponDetailDto getCouponDetailDto(AtomicInteger counter, Set<String> codes, Tuple2<String, String> tuple) {
        String dateValidated = null;
        String errorMessage;
        String bonoEnviado;
        String bonoForObject;


        errorMessage = getErrorMessage(tuple, codes);
        if (errorMessage == null) {
            dateValidated = tuple.getT2();
        }

        bonoEnviado = tuple.getT1();
        bonoForObject = getBonoForObject(bonoEnviado);

        return CouponDetailDto.aCouponDetailDto()
                .withCode(bonoForObject)
                .withDueDate(dateValidated)
                .withNumberLine(counter.incrementAndGet())
                .withMessageError(errorMessage)
                .withTotalLinesFile(1)
                .build();
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
        return bonoIn.chars().allMatch(Character::isDigit) && length12or13(bonoIn);
    }

    public static Boolean length12or13(String bonoIn){
        return bonoIn.length() >= 12 && bonoIn.length() <= 13;
    }

    public static Boolean bonoEAN39(String bonoIn){
        return bonoIn.startsWith("*") && bonoReplace(bonoIn);
    }

    public static Boolean bonoReplace(String bonoIn){
        return bonoIn.replace("*", "").length() >= 1
                && bonoIn.replace("*", "").length() <= 43;
    }

    public static String getErrorMessage(Tuple2<String,String> tuple,Set<String> codes){
        String errorMessage = null;
        if (errorColumnEmpty(tuple)) {
            errorMessage = ExperienceErrorsEnum.FILE_ERROR_COLUMN_EMPTY.toString();
        }
        if (!codes.add(tuple.getT1())) {
            errorMessage = ExperienceErrorsEnum.FILE_ERROR_CODE_DUPLICATE.toString();
        }
        if (!validateDateRegex(tuple.getT2())) {
            errorMessage = ExperienceErrorsEnum.FILE_ERROR_DATE_PARSE.toString();
        }
        if (validateDateIsMinor(tuple.getT2())) {
            errorMessage = ExperienceErrorsEnum.FILE_DATE_IS_MINOR_OR_EQUALS.toString();
        }
        return errorMessage;
    }

    public static boolean errorColumnEmpty(Tuple2<String,String> tuple){
        return tuple.getT1().isBlank() || tuple.getT2().isBlank();
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

    public static String getBonoForObject(String bonoEnviado){
        String bonoForObject = null;
        if (bonoAnteriorNullOrEmpty()) {
            anteriorBono = typeBono(bonoEnviado);
            bonoForObject = anteriorBono.equals("") ? null : bonoEnviado;
        }
        if (anteriorBono.equals(typeBono(bonoEnviado))) {
            bonoForObject = bonoEnviado;
        }
        return bonoForObject;
    }

    public static boolean bonoAnteriorNullOrEmpty(){
        return anteriorBono == null || anteriorBono.equals("");
    }

}
