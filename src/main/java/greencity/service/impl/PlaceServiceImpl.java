package greencity.service.impl;

import greencity.constant.AppConstant;
import greencity.constant.ErrorMessage;
import greencity.constant.LogMessage;
import greencity.dto.place.PlaceAddDto;
import greencity.entity.*;
import greencity.dto.place.AdminPlaceDto;
import greencity.entity.Place;
import greencity.entity.enums.PlaceStatus;
import greencity.mapping.PlaceAddDtoMapper;
import greencity.exception.NotFoundException;
import greencity.exception.PlaceStatusException;
import greencity.repository.PlaceRepo;
import greencity.service.*;

import greencity.service.DateTimeService;
import greencity.service.PlaceService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

/** The class provides implementation of the {@code PlaceService}. */
@Slf4j
@Service
@AllArgsConstructor
public class PlaceServiceImpl implements PlaceService {

    /** Autowired repository. */
    private PlaceRepo placeRepo;

    /** Autowired mapper. */
    private ModelMapper modelMapper;

    private CategoryService categoryService;

    private LocationService locationService;

    private OpenHoursService openingHoursService;

    private PlaceAddDtoMapper placeAddDtoMapper;

    /**
     * Finds all {@code Place} with status {@code PlaceStatus}.
     *
     * @param placeStatus a value of {@link PlaceStatus} enum.
     * @return a list of {@code Place} with the given {@code placeStatus}
     * @author Roman Zahorui
     */
    @Override
    public List<AdminPlaceDto> getPlacesByStatus(PlaceStatus placeStatus) {
        List<Place> places = placeRepo.findAllByStatusOrderByModifiedDateDesc(placeStatus);
        return places.stream()
                .map(place -> modelMapper.map(place, AdminPlaceDto.class))
                .collect(Collectors.toList());
    }

    /**
     * Method for saving proposed Place to database.
     *
     * @param dto - dto for Place entity
     * @return place
     * @author Kateryna Horokh
     */
    @Transactional
    @Override
    public Place save(PlaceAddDto dto) {
        log.info("in save(PlaceAddDto dto), save place - {}", dto.getName());
        Category category = createCategoryByName(dto.getCategory().getName());
        Place place = placeRepo.save(placeAddDtoMapper.convertToEntity(dto));
        place.setCategory(category);
        setPlaceToLocation(place);
        setPlaceToOpeningHours(place);

        return place;
    }

    /**
     * Method for setting OpeningHours entity with Place to database.
     *
     * @param place - Place entity
     * @author Kateryna Horokh
     */
    private void setPlaceToOpeningHours(Place place) {
        log.info("in setPlaceToOpeningHours(Place place)", place.getName());
        List<OpeningHours> hours = place.getOpeningHoursList();
        hours.forEach(
                h -> {
                    h.setPlace(place);
                    openingHoursService.save(h);
                });
    }

    /**
     * Method for setting Location entity with Place to database.
     *
     * @param place - Place entity
     * @author Kateryna Horokh
     */
    private void setPlaceToLocation(Place place) {
        log.info("in setPlaceToLocation(Place place)", place.getName());
        Location location = place.getLocation();
        location.setPlace(place);
        locationService.save(location);
    }

    /**
     * Method for creating new category to database if it does not exists by name.
     *
     * @param name - String category's name
     * @return category
     * @author Kateryna Horokh
     */
    private Category createCategoryByName(String name) {
        log.info("in setPlaceToLocation(Place place)", name);

        Category category = categoryService.findByName(name);
        if (category == null) {
            category = new Category();
            category.setName(name);
            category = categoryService.save(category);
        }
        return category;
    }

    /**
     * Method for deleting place by id.
     *
     * @param id - Long place's id
     * @return boolean
     */
    @Override
    public Boolean deleteById(Long id) {
        log.info("In deleteById() place method.");
        Place place = findById(id);
        placeRepo.delete(place);
        log.info("This place was deleted.");
        return true;
    }

    @Override
    public List<Place> findAll() {
        log.info("In findAll() place method.");
        return placeRepo.findAll();
    }

    /**
     * Update status for the Place and set the time of modification.
     *
     * @param placeId - place id.
     * @param placeStatus - enum of Place status value.
     * @return saved Place entity.
     * @author Nazar Vladyka.
     */
    @Override
    public Place updateStatus(Long placeId, PlaceStatus placeStatus) {
        log.info(LogMessage.IN_UPDATE_PLACE_STATUS, placeId, placeStatus.toString());

        Place updatable = findById(placeId);

        if (updatable.getStatus().equals(placeStatus)) {
            log.error(LogMessage.PLACE_STATUS_NOT_DIFFERENT, placeId, placeStatus);
            throw new PlaceStatusException(
                    ErrorMessage.PLACE_STATUS_NOT_DIFFERENT + updatable.getStatus());
        } else {
            updatable.setStatus(placeStatus);
            updatable.setModifiedDate(DateTimeService.getDateTime(AppConstant.UKRAINE_TIMEZONE));
        }

        return placeRepo.save(updatable);
    }

    /**
     * Find place by it's id.
     *
     * @param id - place id.
     * @return Place entity.
     * @author Nazar Vladyka.
     */
    @Override
    public Place findById(Long id) {
        log.info(LogMessage.IN_FIND_BY_ID, id);

        return placeRepo
                .findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.PLACE_NOT_FOUND_BY_ID + id));
    }

    /**
     * Save place to database.
     *
     * @param place - Place entity.
     * @return saved Place entity.
     * @author Nazar Vladyka.
     */
    @Override
    public Place save(Place place) {
        log.info("in save(Place place), save place - {}", place.getName());

        return placeRepo.saveAndFlush(place);
    }

    @Override
    public Place update(Place place) {
        return null;
    }
}