package com.example.spb.quote;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/quote")
public class QuoteController {

	private final QuoteService service;

	public QuoteController(QuoteService service) {
		this.service = service;
	}

	/** GET /api/quote            (cached)  |  GET /api/quote?fresh=true  (skip cache) */
	@GetMapping
	public Quote quote(@RequestParam(name = "fresh", defaultValue = "false") boolean fresh) {
		return service.get(fresh);
	}
}
