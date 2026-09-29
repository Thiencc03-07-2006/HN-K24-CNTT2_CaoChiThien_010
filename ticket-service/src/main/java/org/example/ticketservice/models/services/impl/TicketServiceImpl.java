package org.example.ticketservice.models.services.impl;

import lombok.RequiredArgsConstructor;
import org.example.ticketservice.models.constants.TicketStatus;
import org.example.ticketservice.models.dto.requests.CreateTicketDetailRequest;
import org.example.ticketservice.models.dto.requests.CreateTicketRequest;
import org.example.ticketservice.models.dto.responses.TicketDetailResponse;
import org.example.ticketservice.models.dto.responses.TicketResponse;
import org.example.ticketservice.models.dto.responses.TripResponse;
import org.example.ticketservice.models.entities.Ticket;
import org.example.ticketservice.models.entities.TicketDetail;
import org.example.ticketservice.models.repositories.TicketDetailRepository;
import org.example.ticketservice.models.repositories.TicketRepository;
import org.example.ticketservice.models.services.TicketService;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {
    private final TicketRepository ticketRepository;
    private final TicketDetailRepository ticketDetailRepository;
    private final TripGatewayService tripGatewayService;

    @Override
    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request) {
            List<ResolvedItem> resolvedItems = request.items().stream()
                            .map(this::resolveTrip)
                            .toList();

            double total = resolvedItems.stream()
                            .mapToDouble(item -> item.trip().ticketPrice() * item.request().seat())
                            .sum();

            Ticket ticket = ticketRepository.save(Ticket.builder()
                            .passengerName(request.passengerName().trim())
                            .passengerEmail(request.passengerEmail().trim())
                            .totalAmount(total)
                            .status(TicketStatus.PENDING)
                            .build());

            List<TicketDetail> details = resolvedItems.stream()
                            .map(item -> TicketDetail.builder()
                                            .ticket(ticket)
                                            .tripId(item.trip().id())
                                            .seats(item.request().seat())
                                            .ticketPrice(item.trip().ticketPrice())
                                            .build())
                            .toList();

            List<TicketDetail> savedDetails = ticketDetailRepository.saveAll(details);
            List<TicketDetailResponse> itemResponses = getTicketDetailResponses(savedDetails, resolvedItems);

            return new TicketResponse(
                            ticket.getId(),
                            ticket.getPassengerName(),
                            ticket.getPassengerEmail(),
                            ticket.getTotalAmount(),
                            ticket.getStatus(),
                            itemResponses);
    }

    private static @NonNull List<TicketDetailResponse> getTicketDetailResponses(List<TicketDetail> savedDetails,
                    List<ResolvedItem> resolvedItems) {
            List<TicketDetailResponse> itemResponses = new ArrayList<>(savedDetails.size());

            for (int index = 0; index < savedDetails.size(); index++) {
                    TicketDetail detail = savedDetails.get(index);
                    TripResponse trip = resolvedItems.get(index).trip();
                    itemResponses.add(new TicketDetailResponse(
                                    detail.getId(),
                                    detail.getTripId(),
                                    detail.getSeats(),
                                    detail.getTicketPrice(),
                                    detail.getTicketPrice() * detail.getSeats()));
            }
            return itemResponses;
    }

    private ResolvedItem resolveTrip(CreateTicketDetailRequest item) {
            TripResponse trip = tripGatewayService.getTripById(item.tripId());
            return new ResolvedItem(item, trip);
    }

    private record ResolvedItem(CreateTicketDetailRequest request, TripResponse trip) {
    }
}
