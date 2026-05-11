package com.streamly.streamly.domain.advertiser.entity;

public enum AdObjectCategory {
    FASHION, FOOD, ELECTRONICS, BEAUTY, FURNITURE, SPORTS, CAR, PET, BOOK, TOY;

    public String toSam3Prompt() {
        return switch (this) {
            case FASHION     -> "clothing, shoes, bag, accessory";
            case FOOD        -> "food, drink, beverage, bottle";
            case ELECTRONICS -> "phone, laptop, electronic device, gadget";
            case BEAUTY      -> "cosmetic, perfume, skincare product";
            case FURNITURE   -> "furniture, chair, table, sofa";
            case SPORTS      -> "sports equipment, ball, racket";
            case CAR         -> "car, vehicle, automobile";
            case PET         -> "pet, dog, cat, animal";
            case BOOK        -> "book, magazine";
            case TOY         -> "toy, doll, figure";
        };
    }
}
