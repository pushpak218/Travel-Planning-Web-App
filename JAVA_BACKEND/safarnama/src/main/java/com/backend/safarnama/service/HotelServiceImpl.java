package com.backend.safarnama.service;

import java.io.IOException;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialException;

import org.apache.tomcat.util.codec.binary.Base64;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.backend.safarnama.exception.PhotoRetrivalException;
import com.backend.safarnama.exception.ResourceNotFoundException;
import com.backend.safarnama.model.Hotel;
import com.backend.safarnama.model.Room;
import com.backend.safarnama.repository.HotelRepository;
import com.backend.safarnama.repository.RoomRepository;
import com.backend.safarnama.response.HotelResponse;
import com.backend.safarnama.response.RoomResponse;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class HotelServiceImpl implements IHotelService{
	
	@Autowired
	private final HotelRepository hotelRepository;
	@Autowired
	private final RoomRepository roomRepository;
	@Autowired
	private final ModelMapper mapper;

	@Override
	public void deleteHotel(Long hotelId) {
		hotelRepository.deleteById(hotelId);
		
	}

	@Override
	public Object getAllHotels() {
		List<HotelResponse> response = hotelRepository.findAll().stream().map(r->{
			byte[] photoBytes = new byte[0];
			try {
				photoBytes=r.getImage().getBytes(1,(int) r.getImage().length());
			} catch (SQLException e) {
				throw new PhotoRetrivalException(e.getMessage());
			}    
			HotelResponse hotelResponse= mapper.map(r,HotelResponse.class);
			hotelResponse.setImage(Base64.encodeBase64String(photoBytes));
			hotelResponse.setRooms(getAllRoomsByHotelId(r));
				
			return hotelResponse;
		}).collect(Collectors.toList());
		return response;
	}

	private List<RoomResponse> getAllRoomsByHotelId(Hotel h) {
		List<RoomResponse> responses = roomRepository.findAllByHotel(h).stream().map(r->{
			byte[] photoBytes = new byte[0];
			try {
				photoBytes=r.getPhoto().getBytes(1,(int) r.getPhoto().length());
			} catch (SQLException e) {
				throw new PhotoRetrivalException(e.getMessage());
			}    
			RoomResponse roomResponse= mapper.map(r,RoomResponse.class);
			roomResponse.setPhoto(Base64.encodeBase64String(photoBytes));
			return roomResponse;
		}).toList();
		return responses;
	}

	@Override
	public List<String> getAllHotelCitys() {
		List<String> citys=hotelRepository.findDistinctHotelCity();
		return citys;
	}

	@Override
	public HotelResponse getHotelById(Long hotelId) throws SQLException {
		Hotel h=hotelRepository.findById(hotelId).orElseThrow(()->new ResourceNotFoundException("Hotel not found"));
		HotelResponse r= mapper.map(h, HotelResponse.class);
		r.setImage(Base64.encodeBase64String(h.getImage().getBytes(1, (int) h.getImage().length())));
		return r;
	}

	@Override
	public Hotel addNewHotel(MultipartFile image, String name, String contactNo, String email, String street,
			String city, String state, String password) throws SerialException, SQLException, IOException {
		Hotel req =new Hotel();
		if(!image.isEmpty()) {
			byte[] photoBytes=image.getBytes();
			Blob photoBlob = new SerialBlob(photoBytes);
			req.setImage(photoBlob);
		}
		req.setName(name);
		req.setContactNo(contactNo);
		req.setEmail(email);
		req.setStreet(street);
		req.setCity(city);
		req.setState(state);
		req.setPassword(password);
		return hotelRepository.save(req);
	}

	@Override
	public Hotel updateHotel(Long hotelId, MultipartFile image, String name, String contactNo, String email,
			String street, String city, String state, String password) throws IOException, SerialException, SQLException {
		Hotel hotel=new Hotel();
		hotel.setId(hotelId);
		hotel.setName(name);
		hotel.setContactNo(contactNo);
		hotel.setEmail(email);
		hotel.setStreet(street);
		hotel.setCity(city);
		hotel.setState(state);
		hotel.setPassword(password);
		
		if(!image.isEmpty()) {
			byte[] photoBytes=image.getBytes();
			Blob photoBlob = new SerialBlob(photoBytes);
			hotel.setImage(photoBlob);
		}
		return hotelRepository.save(hotel);
	}
	
}
