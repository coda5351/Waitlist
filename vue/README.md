# Waitlist

A Vue 3 + TypeScript project for building a Customer Relationship Management system with a modern web framework.

## Getting Started

### Prerequisites
- Node.js (v16 or higher)
- npm or yarn

### Installation

1. Install dependencies:
```bash
npm install
```

### Development

Run the development server:
```bash
npm run dev
```

The application will be available at `http://localhost:5173`

### Build for Production

```bash
npm run build
```

### Preview Production Build

```bash
npm run preview
```

### Type Check

```bash
npm run type-check
```

### End-to-End Testing (Cypress)

This project uses [Cypress](https://www.cypress.io) for end-to-end tests.

1. Install dependencies (if you haven't already):
   ```bash
   npm install
   ```
2. Launch the Vite dev server in one terminal:
   ```bash
   npm run dev
   ```
3. In another terminal, open the Cypress test runner:
   ```bash
   npm run cypress:open
   ```

Alternatively you can run all specs headlessly:

```bash
npm run cypress:run
```

Tests live under `cypress/e2e` and support files under `cypress/support`.

## Project Structure

```
src/
├── components/     # Reusable Vue components
├── App.vue        # Root component
├── main.ts        # Application entry point
└── ...
```

## Features

- ✨ Vue 3 with Composition API
- 🎯 TypeScript support
- ⚡ Vite for fast development and building
- 📦 Modern tooling and best practices

## License

MIT
