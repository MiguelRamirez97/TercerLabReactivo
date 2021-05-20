package com.example;


import reactor.core.publisher.Flux;

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
        return createFluxFrom(fileBase64).map(formatCupon -> createCuponFormat(formatCupon
                .split(characterSeparated))).map(cupon -> getCouponDetailDto(counter,codes,cupon));
    }

    private static Flux<String> createFluxFrom(String fileBase64) {
        return Flux.using(() -> new BufferedReader(new InputStreamReader(
                        new ByteArrayInputStream(decodeBase64(fileBase64))
                )).lines().skip(1),
                Flux::fromStream,
                Stream::close
        );
    }

    private static ModeloCupon createCuponFormat(String[] array){
        return new ModeloCupon(array[0],array[1]);
    }

    private static CouponDetailDto getCouponDetailDto(AtomicInteger counter, Set<String> codes, ModeloCupon cupon) {
        String dateValidated = null;
        String bonoEnviado;
        String bonoForObject;

        String errorMessage = getErrorMessage(cupon, codes);
        if (errorMessage == null) {
            dateValidated = cupon.getDate();
        }

        bonoEnviado = cupon.getBono();
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
        return bonoEAN13(bonoIn)
                ? ValidateCouponEnum.EAN_13.getTypeOfEnum()
                : (bonoEAN39(bonoIn))
                ? ValidateCouponEnum.EAN_39.getTypeOfEnum()
                : ValidateCouponEnum.ALPHANUMERIC.getTypeOfEnum();
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

    public static String getErrorMessage(ModeloCupon cupon,Set<String> codes){
        Map<String, Boolean> error = new LinkedHashMap<>();
        error.put(ExperienceErrorsEnum.FILE_ERROR_COLUMN_EMPTY.toString(), errorColumnEmpty(cupon));
        error.put(ExperienceErrorsEnum.FILE_ERROR_CODE_DUPLICATE.toString(), (!codes.add(cupon.getBono())));
        error.put(ExperienceErrorsEnum.FILE_ERROR_DATE_PARSE.toString(), (!validateDateRegex(cupon.getDate())));
        error.put(ExperienceErrorsEnum.FILE_DATE_IS_MINOR_OR_EQUALS.toString(), validateDateIsMinor(cupon.getDate()));
        for (Map.Entry<String, Boolean> errorbono : error.entrySet()){
            if(errorbono.getValue()){
                return errorbono.getKey();
            }
        }
        return null;
    }

    public static boolean errorColumnEmpty(ModeloCupon cupon){
        return cupon.getBono().equals(EMPTY_STRING) || cupon.getDate().equals(EMPTY_STRING);
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
