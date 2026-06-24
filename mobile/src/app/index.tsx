import { Button, Container, Text, YContainer } from "@primo-brutality/ui";

export default function HomeScreen() {
  return (
    <Container centered className="flex-1 bg-brutal-cream px-6">
      <YContainer gap="md" align="center">
        <Text size="xl" weight="bold">
          My Routine
        </Text>
        <Text size="sm" color="muted">
          UI neo-brutalista configurada
        </Text>
        <Button variant="primary" onPress={() => {}}>
          Começar
        </Button>
      </YContainer>
    </Container>
  );
}
