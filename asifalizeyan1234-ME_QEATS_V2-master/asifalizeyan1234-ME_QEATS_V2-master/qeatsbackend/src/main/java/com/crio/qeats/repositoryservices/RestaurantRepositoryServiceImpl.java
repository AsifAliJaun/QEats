/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.repositoryservices;

import ch.hsr.geohash.GeoHash;
import com.crio.qeats.dto.Restaurant;
import com.crio.qeats.globals.GlobalConstants;
import com.crio.qeats.models.RestaurantEntity;
import com.crio.qeats.repositories.RestaurantRepository;
import com.crio.qeats.utils.GeoLocation;
import com.crio.qeats.utils.GeoUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import org.springframework.stereotype.Service;


@Service
public class RestaurantRepositoryServiceImpl implements RestaurantRepositoryService {


  @Autowired
  private RestaurantRepository restaurantRepository;

  @Autowired
  private MongoTemplate mongoTemplate;

  @Autowired
  private Provider<ModelMapper> modelMapperProvider;

  public static final int MIN_PRECISION = 1;

  // TODO: CRIO_TASK_MODULE_NOSQL
  // Objectives:
  // 1. Implement findAllRestaurantsCloseby.
  // 2. Remember to keep the precision of GeoHash in mind while using it as a key.
  // Check RestaurantRepositoryService.java file for the interface contract.
  public List<Restaurant> findAllRestaurantsCloseBy(Double latitude, Double longitude,
  LocalTime currentTime, Double servingRadiusInKms) {
String geoHash = GeoHash.geoHashStringWithCharacterPrecision(latitude, longitude, MIN_PRECISION);

Query query = new Query(Criteria.where("geohashPrefix").regex("^" + geoHash));
List<RestaurantEntity> restaurantEntities = restaurantRepository.findAll();

List<RestaurantEntity> closeByAndOpenRestaurants = restaurantEntities.stream()
    .filter(restaurantEntity -> isRestaurantCloseByAndOpen(restaurantEntity, currentTime,
        latitude, longitude, servingRadiusInKms))
    .collect(Collectors.toList());

ModelMapper modelMapper = modelMapperProvider.get();
List<Restaurant> restaurants = closeByAndOpenRestaurants.stream()
    .map(restaurantEntity -> modelMapper.map(restaurantEntity, Restaurant.class))
    .collect(Collectors.toList());

return restaurants;
}

private boolean isRestaurantCloseByAndOpen(RestaurantEntity restaurantEntity,
  LocalTime currentTime, Double latitude, Double longitude, Double servingRadiusInKms) {
if (!isOpenNow(currentTime, restaurantEntity)) {
  return false;
}

double distanceInKm = GeoLocation.distanceInKm(latitude, longitude,
    restaurantEntity.getLatitude(), restaurantEntity.getLongitude());

return distanceInKm <= servingRadiusInKms;
}

private boolean isOpenNow(LocalTime time, RestaurantEntity res) {
LocalTime openingTime = LocalTime.parse(res.getOpensAt());
LocalTime closingTime = LocalTime.parse(res.getClosesAt());

if (closingTime.isBefore(openingTime)) {
  closingTime = closingTime.plusHours(24);
}

return time.isAfter(openingTime) && time.isBefore(closingTime);
}
}