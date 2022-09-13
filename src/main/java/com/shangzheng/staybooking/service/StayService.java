package com.shangzheng.staybooking.service;

import com.shangzheng.staybooking.exception.StayDeleteException;
import com.shangzheng.staybooking.exception.StayNotExistException;
import com.shangzheng.staybooking.model.*;
import com.shangzheng.staybooking.repository.LocationRepository;
import com.shangzheng.staybooking.repository.ReservationRepository;
import com.shangzheng.staybooking.repository.StayRepository;
import com.shangzheng.staybooking.repository.StayReservationDateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class StayService {
    private StayRepository stayRepository;

    private ImageStorageService imageStorageService;
    private GeoCodingService geoCodingService;
    private LocationRepository locationRepository;
    private StayReservationDateRepository stayReservationDateRepository;
    private ReservationRepository reservationRepository;

    @Autowired
    public StayService(StayRepository stayRepository,
                       ImageStorageService imageStorageService,
                       GeoCodingService geoCodingService,
                       LocationRepository locationRepository,
                       StayReservationDateRepository stayReservationDateRepository,
                       ReservationRepository reservationRepository) {
        this.stayRepository = stayRepository;
        this.imageStorageService = imageStorageService;
        this.geoCodingService = geoCodingService;
        this.locationRepository = locationRepository;
        this.stayReservationDateRepository = stayReservationDateRepository;
        this.reservationRepository = reservationRepository;
    }


    public List<Stay> listByUser(String username) {
        return stayRepository.findByHost(new User.Builder().setUsername(username).build());
    }

    public Stay findByIdAndHost(Long stayId, String username) throws StayNotExistException {
        Stay stay = stayRepository.findByIdAndHost(stayId, new User.Builder().setUsername(username).build());
        if (stay == null) {
            throw new StayNotExistException("Stay doesn't exist");
        }
        return stay;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void add(Stay stay, MultipartFile[] images) {
        List<String> mediaLinks = Arrays.stream(images).parallel().map(image -> imageStorageService.save(image)).collect(Collectors.toList());
        List<StayImage> stayImages = new ArrayList<>();
        for (String mediaLink : mediaLinks) {
            stayImages.add(new StayImage(mediaLink, stay));
        }
        stay.setImages(stayImages);

        stayRepository.save(stay);
        Location location = geoCodingService.getLatLng(stay.getId(), stay.getAddress());
        locationRepository.save(location);

    }


    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void delete(Long stayId, String username) throws StayNotExistException {
        Stay stay = stayRepository.findByIdAndHost(stayId, new User.Builder().setUsername(username).build());
        if (stay == null) {
            throw new StayNotExistException("Stay doesn't exist");
        }
        List<Reservation> reservations = reservationRepository.findByStayAndCheckoutDateAfter(stay, LocalDate.now());
        if (reservations != null && reservations.size() > 0) {
            throw new StayDeleteException("Cannot delete stay with active reservation");
        }
        List<StayReservedDate> stayReservedDates = stayReservationDateRepository.findByStay(stay);

        for (StayReservedDate date : stayReservedDates) {
            stayReservationDateRepository.deleteById(date.getId());
        }

        stayRepository.deleteById(stayId);
    }

}
