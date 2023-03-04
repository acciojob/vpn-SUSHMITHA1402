package com.driver.services.impl;

import com.driver.model.*;
import com.driver.repository.ConnectionRepository;
import com.driver.repository.ServiceProviderRepository;
import com.driver.repository.UserRepository;
import com.driver.services.ConnectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ConnectionServiceImpl implements ConnectionService {
    @Autowired
    UserRepository userRepository2;
    @Autowired
    ServiceProviderRepository serviceProviderRepository2;
    @Autowired
    ConnectionRepository connectionRepository2;

    @Override
    public User connect(int userId, String countryName) throws Exception{
        //1. If the user is already connected to any service provider, throw "Already connected" exception.
        User user = userRepository2.findById(userId).get();
        if(user.getConnected()) throw new Exception("Already connected");

        //2. Else if the countryName corresponds to the original country of the user, do nothing.
        // This means that the user wants to connect to its original country, for which we do not require a connection.
        // Thus, return the user as it is.
        String originalCountry = user.getOriginalCountry().getCountryName().toString();
        if(originalCountry.equals(countryName)) return user;

        //3. Else, the user should be subscribed under a serviceProvider having option to connect to the given country.
        //If the connection can not be made (As user does not have a serviceProvider or serviceProvider does not have given country,
        // throw "Unable to connect" exception.

        ServiceProvider possibleServiceProvider = null;
        String newCode ="";
        countryName = countryName.toUpperCase();
        List<ServiceProvider> serviceProviderList = user.getServiceProviderList();
        if(serviceProviderList==null) throw new Exception("Unable to connect");
        for (ServiceProvider serviceProvider:serviceProviderList){
            List<Country> countryList = serviceProvider.getCountryList();
            for (Country country:countryList){
                if(country.getCountryName().toString().equals(countryName)){
                    if (possibleServiceProvider==null){
                        newCode = country.getCode();
                        possibleServiceProvider = serviceProvider;
                    }
                    if(serviceProvider.getId()<possibleServiceProvider.getId()) {
                       possibleServiceProvider = serviceProvider;
                    }

                }
            }
        }
        if (possibleServiceProvider==null) throw new Exception("Unable to connect");
        user.setMaskedIp(newCode+"."+possibleServiceProvider.getId()+"."+user.getId());
        user.setConnected(true);

        Connection connection = new Connection();
        connection.setUser(user);
        connection.setServiceProvider(possibleServiceProvider);

        List<Connection> connections = user.getConnectionList();
        connections.add(connection);
        user.setConnectionList(connections);

        userRepository2.save(user);

        return user;
        //Else, establish the connection where the maskedIp is "updatedCountryCode.serviceProviderId.userId" and return the updated user.
        // If multiple service providers allow you to connect to the country, use the service provider having smallest id.

    }
    @Override
    public User disconnect(int userId) throws Exception {
        User user = userRepository2.findById(userId).get();
        //If the given user was not connected to a vpn, throw "Already disconnected" exception.
        if(user.getConnected()==Boolean.FALSE) throw new Exception("Already disconnected");
        //Else, disconnect from vpn, make masked Ip as null, update relevant attributes and return updated user.
        user.setConnected(false);
        user.setMaskedIp(null);

        userRepository2.save(user);
        return user;
    }
    @Override
    public User communicate(int senderId, int receiverId) throws Exception {
        //Establish a connection between sender and receiver users
        User sender = userRepository2.findById(senderId).get();
        User receiver = userRepository2.findById(receiverId).get();
        //To communicate to the receiver, sender should be in the current country of the receiver.
        //If the receiver is connected to a vpn, his current country is the one he is connected to.
        //If the receiver is not connected to vpn, his current country is his original country.
        String receiverCountry ="";
        if(receiver.getConnected()==Boolean.FALSE) receiverCountry = receiver.getOriginalCountry().getCode();
        else{
            receiverCountry = receiver.getMaskedIp().substring(0,3);
        }

        //If the sender's original country matches receiver's current country,
        // we do not need to do anything as they can communicate. Return the sender as it is.
        if(sender.getOriginalCountry().getCode().equals(receiverCountry)) return sender;

        //If the sender's original country does not match receiver's current country,
        // we need to connect the sender to a suitable vpn. If there are multiple options,
        // connect using the service provider having smallest id
        //If communication can not be established due to any reason, throw "Cannot establish communication" exception
        try {
            String countryName = "";
            if (receiverCountry.equals(CountryName.AUS.toCode())) countryName="AUS";
            else if (receiverCountry.equals(CountryName.USA.toCode())) countryName="USA";
            else if (receiverCountry.equals(CountryName.IND.toCode())) countryName="IND";
            else if (receiverCountry.equals(CountryName.CHI.toCode())) countryName="CHI";
            else if (receiverCountry.equals(CountryName.JPN.toCode())) countryName="JPN";
            sender = connect(senderId,countryName);
            return sender;
        }
        catch (Exception e){
            throw new Exception("Cannot establish communication");
        }


    }
}
