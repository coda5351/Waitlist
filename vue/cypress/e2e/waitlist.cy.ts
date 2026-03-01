describe('Waitlist page', () => {
  beforeEach(() => {
    cy.intercept('GET', '**/accounts/*/waitlist-status', {
      enabled: true,
      open: true,
      estimatedWait: 5
    }).as('status')
    cy.intercept('GET', '**/entries', []).as('list')
    cy.visit('/waitlist')
  })

  it('shows waitlist information and form', () => {
    cy.contains('Join the Waitlist')
    cy.contains('Current status').should('exist')
    cy.get('#name, input').should('exist') // at least one input
  })

  it('allows a user to sign up', () => {
    cy.get('#name, input').first().type('Cypress User')
    cy.get('#phone, input').eq(1).type('123-456-7890')
    cy.get('input[type=number]').clear().type('2')
    cy.intercept('POST', '**/entries', { code: 'abc123' }).as('join')
    cy.get('button').contains('Join').click()
    cy.wait('@join')
    cy.url().should('include', '/waitlist/abc123')
  })
})