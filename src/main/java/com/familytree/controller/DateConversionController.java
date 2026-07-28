package com.familytree.controller;

import com.familytree.calendar.BikramSambatConverter;
import com.familytree.calendar.BikramSambatDate;
import com.familytree.dto.AdDateDto;
import com.familytree.dto.BsDateDto;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Live AD<->BS date conversion for the signup form and anywhere else a
 * person enters a date in either calendar (docs/13 "AD/BS date
 * strategy"). Public and unauthenticated -- needed before an applicant
 * has an account, and reveals nothing about any person or family data.
 */
@RestController
@RequestMapping("/api/v1/date-conversion")
public class DateConversionController {

    @GetMapping("/bs-to-ad")
    public AdDateDto bsToAd(@RequestParam int year, @RequestParam int month, @RequestParam int day) {
        LocalDate adDate = BikramSambatConverter.toAd(new BikramSambatDate(year, month, day));
        return new AdDateDto(adDate);
    }

    @GetMapping("/ad-to-bs")
    public BsDateDto adToBs(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        BikramSambatDate bsDate = BikramSambatConverter.toBs(date);
        return new BsDateDto(bsDate.year(), bsDate.month(), bsDate.day());
    }
}
